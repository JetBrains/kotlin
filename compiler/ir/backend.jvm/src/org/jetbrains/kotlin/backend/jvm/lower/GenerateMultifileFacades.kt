/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.SameTypeCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.namedIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

internal val generateMultifileFacadesPhase = namedIrModulePhase(
    name = "GenerateMultifileFacades",
    description = "Generate JvmMultifileClass facades, based on the information provided by FileClassLowering",
    prerequisite = setOf(fileClassPhase),
    lower = object : SameTypeCompilerPhase<JvmBackendContext, IrModuleFragment> {
        override fun invoke(
            phaseConfig: PhaseConfig,
            phaserState: PhaserState<IrModuleFragment>,
            context: JvmBackendContext,
            input: IrModuleFragment
        ): IrModuleFragment {
            val functionDelegates = mutableMapOf<IrFunction, IrFunction>()

            // In -Xmultifile-parts-inherit mode, instead of generating "bridge" methods in the facade which call into parts,
            // we construct an inheritance chain such that all part members are present as fake overrides in the facade.
            val shouldGeneratePartHierarchy = context.state.languageVersionSettings.getFlag(JvmAnalysisFlags.inheritMultifileParts)
            input.files.addAll(
                generateMultifileFacades(input.descriptor, context, shouldGeneratePartHierarchy, functionDelegates)
            )

            UpdateFunctionCallSites(functionDelegates).lower(input)

            context.multifileFacadesToAdd.clear()

            functionDelegates.entries.associateTo(context.multifileFacadeMemberToPartMember) { (member, newMember) -> newMember to member }

            return input
        }
    }
)

internal class MultifileFacadeFileEntry(
    private val className: JvmClassName,
    val partFiles: List<IrFile>
) : SourceManager.FileEntry {
    override val name: String
        get() = "<multi-file facade $className>"

    override val maxOffset: Int
        get() = UNDEFINED_OFFSET

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo =
        error("Multifile facade doesn't support debug info: $className")

    override fun getLineNumber(offset: Int): Int =
        error("Multifile facade doesn't support debug info: $className")

    override fun getColumnNumber(offset: Int): Int =
        error("Multifile facade doesn't support debug info: $className")
}

private fun generateMultifileFacades(
    module: ModuleDescriptor,
    context: JvmBackendContext,
    shouldGeneratePartHierarchy: Boolean,
    functionDelegates: MutableMap<IrFunction, IrFunction>
): List<IrFile> =
    context.multifileFacadesToAdd.map { (jvmClassName, partClasses) ->
        val kotlinPackageFqName = partClasses.first().fqNameWhenAvailable!!.parent()
        if (!partClasses.all { it.fqNameWhenAvailable!!.parent() == kotlinPackageFqName }) {
            throw UnsupportedOperationException(
                "Multi-file parts of a facade with JvmPackageName should all lie in the same Kotlin package:\n  " +
                        partClasses.joinToString("\n  ") { klass ->
                            "Class ${klass.fqNameWhenAvailable}, JVM name ${context.classNameOverride[klass]}"
                        }
            )
        }

        val fileEntry = MultifileFacadeFileEntry(jvmClassName, partClasses.map(IrClass::fileParent))
        val file = IrFileImpl(fileEntry, EmptyPackageFragmentDescriptor(module, kotlinPackageFqName))

        context.log {
            "Multifile facade $jvmClassName:\n  ${partClasses.joinToString("\n  ") { it.fqNameWhenAvailable!!.asString() }}\n"
        }

        val facadeClass = buildClass {
            name = jvmClassName.fqNameForTopLevelClassMaybeWithDollars.shortName()
        }.apply {
            parent = file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            origin = IrDeclarationOrigin.FILE_CLASS
            if (jvmClassName.packageFqName != kotlinPackageFqName) {
                context.classNameOverride[this] = jvmClassName
            }
            if (shouldGeneratePartHierarchy) {
                val superClass = modifyMultifilePartsForHierarchy(context, partClasses)
                superTypes.add(superClass.typeWith())

                addConstructor {
                    visibility = Visibilities.PRIVATE
                    isPrimary = true
                }.also { constructor ->
                    constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                        +irDelegatingConstructorCall(superClass.primaryConstructor!!)
                    }
                }
            }
        }
        file.declarations.add(facadeClass)

        for (partClass in partClasses) {
            context.multifileFacadeForPart[partClass.attributeOwnerId as IrClass] = jvmClassName

            moveFieldsOfConstProperties(partClass, facadeClass)

            for (member in partClass.declarations) {
                if (member is IrSimpleFunction) {
                    val newMember = member.createMultifileDelegateIfNeeded(context, facadeClass, shouldGeneratePartHierarchy)
                    if (newMember != null) {
                        functionDelegates[member] = newMember
                    }
                }
            }
        }

        file
    }

// Changes supertypes of multifile part classes so that they inherit from each other, and returns the last part class.
// The multifile facade should inherit from that part class.
private fun modifyMultifilePartsForHierarchy(context: JvmBackendContext, unsortedParts: List<IrClass>): IrClass {
    val parts = unsortedParts.sortedBy(IrClass::name)
    val superClasses = listOf(context.irBuiltIns.anyClass.owner) + parts.subList(0, parts.size - 1)

    for ((klass, superClass) in parts.zip(superClasses)) {
        klass.modality = Modality.OPEN
        klass.visibility = JavaVisibilities.PACKAGE_VISIBILITY

        klass.superTypes.clear()
        klass.superTypes.add(superClass.typeWith())

        klass.addConstructor {
            isPrimary = true
        }.also { constructor ->
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(superClass.primaryConstructor!!)
            }
        }
    }

    return parts.last()
}

private fun moveFieldsOfConstProperties(partClass: IrClass, facadeClass: IrClass) {
    partClass.declarations.transformFlat { member ->
        if (member is IrField && member.shouldMoveToFacade()) {
            member.patchDeclarationParents(facadeClass)
            facadeClass.declarations.add(member)
            emptyList()
        } else null
    }
}

private fun IrField.shouldMoveToFacade(): Boolean {
    val property = correspondingPropertySymbol?.owner
    return property != null && property.isConst && !Visibilities.isPrivate(visibility)
}

private fun IrSimpleFunction.createMultifileDelegateIfNeeded(
    context: JvmBackendContext,
    facadeClass: IrClass,
    shouldGeneratePartHierarchy: Boolean
): IrSimpleFunction? {
    if (Visibilities.isPrivate(visibility) ||
        name == StaticInitializersLowering.clinitName ||
        origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
    ) return null

    // TODO: perform copy of the signature only, without body
    val function = deepCopyWithSymbols(facadeClass)

    if (shouldGeneratePartHierarchy) {
        function.body = null
        function.origin = IrDeclarationOrigin.FAKE_OVERRIDE
        function.overriddenSymbols.clear()
        function.overriddenSymbols.add(symbol)
    } else {
        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            +irReturn(irCall(this@createMultifileDelegateIfNeeded).also { call ->
                call.passTypeArgumentsFrom(function)
                function.extensionReceiverParameter?.let { parameter ->
                    call.extensionReceiver = irGet(parameter)
                }
                for (parameter in function.valueParameters) {
                    call.putValueArgument(parameter.index, irGet(parameter))
                }
            })
        }
        function.origin = JvmLoweredDeclarationOrigin.MULTIFILE_BRIDGE
    }

    facadeClass.declarations.add(function)

    return function
}

private class UpdateFunctionCallSites(
    private val functionDelegates: MutableMap<IrFunction, IrFunction>
) : FileLoweringPass, IrElementTransformer<IrFunction?> {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement =
        super.visitFunction(declaration, declaration)

    override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
        if (data?.origin == JvmLoweredDeclarationOrigin.MULTIFILE_BRIDGE)
            return super.visitCall(expression, data)

        val newFunction = functionDelegates[expression.symbol.owner]
            ?: return super.visitCall(expression, data)

        return expression.run {
            // TODO: deduplicate this with ReplaceKFunctionInvokeWithFunctionInvoke
            IrCallImpl(startOffset, endOffset, type, newFunction.symbol).apply {
                copyTypeArgumentsFrom(expression)
                extensionReceiver = expression.extensionReceiver?.transform(this@UpdateFunctionCallSites, null)
                for (i in 0 until valueArgumentsCount) {
                    putValueArgument(i, expression.getValueArgument(i)?.transform(this@UpdateFunctionCallSites, null))
                }
            }
        }
    }
}
