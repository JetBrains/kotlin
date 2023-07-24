/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeCustomPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.backend.jvm.isMultifileBridge
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.JvmNames
import org.jetbrains.kotlin.name.JvmNames.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors

internal val generateMultifileFacadesPhase = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    name = "GenerateMultifileFacades",
    description = "Generate JvmMultifileClass facades, based on the information provided by FileClassLowering",
    prerequisite = setOf(fileClassPhase),
    op = { context, input ->
        val functionDelegates = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

        // In -Xmultifile-parts-inherit mode, instead of generating "bridge" methods in the facade which call into parts,
        // we construct an inheritance chain such that all part members are present as fake overrides in the facade.
        val shouldGeneratePartHierarchy = context.state.languageVersionSettings.getFlag(JvmAnalysisFlags.inheritMultifileParts)
        input.files.addAll(
            generateMultifileFacades(input, context, shouldGeneratePartHierarchy, functionDelegates)
        )

        UpdateFunctionCallSites(functionDelegates).lower(input)
        UpdateConstantFacadePropertyReferences(context, shouldGeneratePartHierarchy).lower(input)

        context.multifileFacadesToAdd.clear()

        functionDelegates.entries.associateTo(context.multifileFacadeMemberToPartMember) { (member, newMember) -> newMember to member }
    }
)

private fun generateMultifileFacades(
    module: IrModuleFragment,
    context: JvmBackendContext,
    shouldGeneratePartHierarchy: Boolean,
    functionDelegates: MutableMap<IrSimpleFunction, IrSimpleFunction>
): List<IrFile> =
    context.multifileFacadesToAdd.map { (jvmClassName, unsortedPartClasses) ->
        val partClasses = unsortedPartClasses.sortedBy(IrClass::name)
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
        val file = IrFileImpl(fileEntry, EmptyPackageFragmentDescriptor(module.descriptor, kotlinPackageFqName), module)

        context.log {
            "Multifile facade $jvmClassName:\n  ${partClasses.joinToString("\n  ") { it.fqNameWhenAvailable!!.asString() }}\n"
        }

        val facadeClass = context.irFactory.buildClass {
            name = jvmClassName.fqNameForTopLevelClassMaybeWithDollars.shortName()
        }.apply {
            parent = file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            origin = IrDeclarationOrigin.JVM_MULTIFILE_CLASS
            if (jvmClassName.packageFqName != kotlinPackageFqName) {
                context.classNameOverride[this] = jvmClassName
            }
            if (shouldGeneratePartHierarchy) {
                val superClass = modifyMultifilePartsForHierarchy(context, partClasses)
                superTypes = listOf(superClass.typeWith())

                addConstructor {
                    visibility = DescriptorVisibilities.PRIVATE
                    isPrimary = true
                }.also { constructor ->
                    constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                        +irDelegatingConstructorCall(superClass.primaryConstructor!!)
                    }
                }
            }

            val nonJvmSyntheticParts = partClasses.filterNot { it.hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) }
            if (nonJvmSyntheticParts.isEmpty()) {
                annotations = annotations + partClasses.first().getAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)!!.deepCopyWithSymbols()
            } else if (nonJvmSyntheticParts.size < partClasses.size) {
                for (part in nonJvmSyntheticParts) {
                    val partFile = part.fileParent
                    // If at least one of parts is annotated with @JvmSynthetic, then all other parts should also be annotated.
                    // We report this error on the `@JvmMultifileClass` annotation of each non-@JvmSynthetic part.
                    val annotation = partFile.annotations.singleOrNull { it.isAnnotationWithEqualFqName(JvmNames.JVM_MULTIFILE_CLASS) }
                    context.ktDiagnosticReporter.at(annotation ?: partFile, partFile).report(
                        JvmBackendErrors.NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC
                    )
                }
            }
        }

        file.declarations.add(facadeClass)

        for (partClass in partClasses) {
            context.multifileFacadeForPart[partClass.attributeOwnerId as IrClass] = jvmClassName
            context.multifileFacadeClassForPart[partClass.attributeOwnerId as IrClass] = facadeClass

            val correspondingProperties = CorrespondingPropertyCache(context, facadeClass)
            for (member in partClass.declarations) {
                if (member !is IrSimpleFunction) continue

                // KT-43519 Don't generate delegates for external methods
                if (member.isExternal) continue

                val correspondingProperty = member.correspondingPropertySymbol?.owner
                if (member.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME) ||
                    correspondingProperty?.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME) == true
                ) continue

                val newMember =
                    member.createMultifileDelegateIfNeeded(context, facadeClass, correspondingProperties, shouldGeneratePartHierarchy)
                if (newMember != null) {
                    functionDelegates[member] = newMember
                }
            }

            moveFieldsOfConstProperties(partClass, facadeClass, correspondingProperties)
        }

        file
    }

// Changes supertypes of multifile part classes so that they inherit from each other, and returns the last part class.
// The multifile facade should inherit from that part class.
private fun modifyMultifilePartsForHierarchy(context: JvmBackendContext, parts: List<IrClass>): IrClass {
    val superClasses = listOf(context.irBuiltIns.anyClass.owner) + parts.subList(0, parts.size - 1)

    for ((klass, superClass) in parts.zip(superClasses)) {
        klass.modality = Modality.OPEN
        klass.visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY

        klass.superTypes = listOf(superClass.typeWith())

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

private fun moveFieldsOfConstProperties(partClass: IrClass, facadeClass: IrClass, correspondingProperties: CorrespondingPropertyCache) {
    partClass.declarations.transformFlat { member ->
        if (member is IrField && member.shouldMoveToFacade()) {
            member.patchDeclarationParents(facadeClass)
            facadeClass.declarations.add(member)
            member.correspondingPropertySymbol?.let { oldPropertySymbol ->
                val newProperty = correspondingProperties.getOrCopyProperty(oldPropertySymbol.owner)
                member.correspondingPropertySymbol = newProperty.symbol
                newProperty.backingField = member
            }
            emptyList()
        } else null
    }
}

private fun IrField.shouldMoveToFacade(): Boolean {
    val property = correspondingPropertySymbol?.owner
    return property != null && property.isConst && !DescriptorVisibilities.isPrivate(visibility)
}

private fun IrSimpleFunction.createMultifileDelegateIfNeeded(
    context: JvmBackendContext,
    facadeClass: IrClass,
    correspondingProperties: CorrespondingPropertyCache,
    shouldGeneratePartHierarchy: Boolean
): IrSimpleFunction? {
    val target = this

    val originalVisibility = context.mapping.defaultArgumentsOriginalFunction[this]?.visibility ?: visibility

    if (DescriptorVisibilities.isPrivate(originalVisibility) ||
        name == StaticInitializersLowering.clinitName ||
        origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR ||
        origin == JvmLoweredDeclarationOrigin.INLINE_LAMBDA ||
        origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA ||
        origin == IrDeclarationOrigin.PROPERTY_DELEGATE ||
        origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR ||
        // $annotations methods in the facade are only needed for const properties.
        (origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS &&
                (metadata as? MetadataSource.Property)?.isConst != true)
    ) return null

    val function = context.irFactory.buildFun {
        updateFrom(target)
        isFakeOverride = shouldGeneratePartHierarchy
        name = target.name
    }

    val targetProperty = correspondingPropertySymbol?.owner
    if (targetProperty != null) {
        val newProperty = correspondingProperties.getOrCopyProperty(targetProperty)
        function.correspondingPropertySymbol = newProperty.symbol
        when (target.valueParameters.size) {
            0 -> newProperty.getter = function
            1 -> newProperty.setter = function
        }
    }

    function.copyAttributes(target)
    function.copyAnnotationsFrom(target)
    function.copyParameterDeclarationsFrom(target)
    function.returnType = target.returnType.substitute(target.typeParameters, function.typeParameters.map { it.defaultType })
    function.parent = facadeClass

    if (shouldGeneratePartHierarchy) {
        function.origin = IrDeclarationOrigin.FAKE_OVERRIDE
        function.body = null
        function.overriddenSymbols = listOf(symbol)
    } else {
        function.overriddenSymbols = overriddenSymbols.toList()
        function.body = context.createIrBuilder(function.symbol).irBlockBody {
            +irReturn(irCall(target).also { call ->
                call.passTypeArgumentsFrom(function)
                function.extensionReceiverParameter?.let { parameter ->
                    call.extensionReceiver = irGet(parameter)
                }
                for (parameter in function.valueParameters) {
                    call.putValueArgument(parameter.index, irGet(parameter))
                }
            })
        }
    }

    facadeClass.declarations.add(function)

    return function
}

private class CorrespondingPropertyCache(private val context: JvmBackendContext, private val facadeClass: IrClass) {
    private var cache: MutableMap<IrProperty, IrProperty>? = null

    fun getOrCopyProperty(from: IrProperty): IrProperty {
        val cache = cache ?: mutableMapOf<IrProperty, IrProperty>().also { cache = it }
        return cache.getOrPut(from) {
            context.irFactory.buildProperty {
                updateFrom(from)
                name = from.name
            }.apply {
                parent = facadeClass
                copyAnnotationsFrom(from)
            }
        }
    }
}

private class UpdateFunctionCallSites(
    private val functionDelegates: MutableMap<IrSimpleFunction, IrSimpleFunction>
) : FileLoweringPass, IrElementVisitor<Unit, IrFunction?> {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildren(this, null)
    }

    override fun visitElement(element: IrElement, data: IrFunction?) {
        element.acceptChildren(this, data)
    }

    override fun visitFunction(declaration: IrFunction, data: IrFunction?): Unit =
        super.visitFunction(declaration, declaration)

    override fun visitCall(expression: IrCall, data: IrFunction?) {
        expression.acceptChildren(this, data)

        if (data == null || !data.isMultifileBridge()) {
            functionDelegates[expression.symbol.owner]?.let {
                expression.symbol = it.symbol
            }
        }
    }
}

private class UpdateConstantFacadePropertyReferences(
    private val context: JvmBackendContext,
    private val shouldGeneratePartHierarchy: Boolean
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val facadeClass = getReplacementFacadeClassOrNull(irClass) ?: return

        // Replace the class reference in the body of the property reference class (in getOwner) to refer to the facade class instead.
        irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement = declaration

            override fun visitClassReference(expression: IrClassReference): IrExpression = IrClassReferenceImpl(
                expression.startOffset, expression.endOffset, facadeClass.defaultType, facadeClass.symbol, facadeClass.defaultType
            )
        })
    }

    // We should replace references to facade classes in the following cases:
    // - if -Xmultifile-parts-inherit is enabled, always replace all references;
    // - otherwise, replace references in classes for properties whose fields were moved to the facade class.
    private fun getReplacementFacadeClassOrNull(irClass: IrClass): IrClass? {
        if (irClass.origin != JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE &&
            irClass.origin != JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
        ) return null

        val declaration = when (val callableReference = irClass.attributeOwnerId) {
            is IrPropertyReference -> callableReference.getter?.owner?.correspondingPropertySymbol?.owner
            is IrFunctionReference -> callableReference.symbol.owner
            else -> null
        } ?: return null
        val parent = declaration.parent as? IrClass ?: return null
        val facadeClass = context.multifileFacadeClassForPart[parent.attributeOwnerId]

        return if (shouldGeneratePartHierarchy ||
            (declaration is IrProperty && declaration.backingField?.shouldMoveToFacade() == true)
        ) facadeClass else null
    }
}
