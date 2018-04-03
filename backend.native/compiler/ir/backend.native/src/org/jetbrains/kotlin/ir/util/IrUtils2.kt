/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.OverridingStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import java.lang.reflect.Proxy

//TODO: delete file on next kotlin dependency update
internal fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

private var topLevelInitializersCounter = 0

internal fun IrFile.addTopLevelInitializer(expression: IrExpression) {
    val fieldDescriptor = PropertyDescriptorImpl.create(
            this.packageFragmentDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PRIVATE,
            false,
            "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false,
            false,
            false,
            false,
            false,
            false
    )

    val builtIns = fieldDescriptor.builtIns
    fieldDescriptor.setType(builtIns.unitType, emptyList(), null, null as KotlinType?)
    fieldDescriptor.initialize(null, null)

    val irField = IrFieldImpl(
            expression.startOffset, expression.endOffset,
            IrDeclarationOrigin.DEFINED, fieldDescriptor
    )

    val initializer = IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset, builtIns.unitType,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, builtIns.unitType, expression
    )

    irField.initializer = IrExpressionBodyImpl(expression.startOffset, expression.endOffset, initializer)

    this.addChild(irField)
}

fun IrClass.addFakeOverrides() {

    val startOffset = this.startOffset
    val endOffset = this.endOffset

    descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .forEach {
                this.addChild(createFakeOverride(it, startOffset, endOffset))
            }
}

private fun createFakeOverride(descriptor: CallableMemberDescriptor, startOffset: Int, endOffset: Int): IrDeclaration {

    fun FunctionDescriptor.createFunction(): IrFunction = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.FAKE_OVERRIDE, this
    ).apply {
        createParameterDeclarations()
    }

    return when (descriptor) {
        is FunctionDescriptor -> descriptor.createFunction()
        is PropertyDescriptor ->
            IrPropertyImpl(startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor).apply {
                // TODO: add field if getter is missing?
                getter = descriptor.getter?.createFunction()
                setter = descriptor.setter?.createFunction()
            }
        else -> TODO(descriptor.toString())
    }
}

fun IrFunction.createParameterDeclarations() {
    fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            innerStartOffset(this), innerEndOffset(this),
            IrDeclarationOrigin.DEFINED,
            this
    ).also {
        it.parent = this@createParameterDeclarations
    }

    dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
    extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

    assert(valueParameters.isEmpty())
    descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

    assert(typeParameters.isEmpty())
    descriptor.typeParameters.mapTo(typeParameters) {
        IrTypeParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.DEFINED,
                it
        ).also { typeParameter ->
            typeParameter.parent = this
        }
    }
}

fun IrSimpleFunction.setOverrides(symbolTable: SymbolTable) {
    assert(this.overriddenSymbols.isEmpty())

    this.descriptor.overriddenDescriptors.mapTo(this.overriddenSymbols) {
        symbolTable.referenceSimpleFunction(it.original)
    }

    this.typeParameters.forEach { it.setSupers(symbolTable) }
}

fun IrClass.simpleFunctions(): List<IrSimpleFunction> = this.declarations.flatMap {
    when (it) {
        is IrSimpleFunction -> listOf(it)
        is IrProperty -> listOfNotNull(it.getter as IrSimpleFunction?, it.setter as IrSimpleFunction?)
        else -> emptyList()
    }
}

fun IrClass.createParameterDeclarations() {
    descriptor.thisAsReceiverParameter.let {
        thisReceiver = IrValueParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                it
        ).also { valueParameter ->
            valueParameter.parent = this
        }
    }

    assert(typeParameters.isEmpty())
    descriptor.declaredTypeParameters.mapTo(typeParameters) {
        IrTypeParameterImpl(
                innerStartOffset(it), innerEndOffset(it),
                IrDeclarationOrigin.DEFINED,
                it
        ).also { typeParameter ->
            typeParameter.parent = this
        }
    }
}

fun IrClass.setSuperSymbols(supers: List<IrClass>) {
    assert(this.superDescriptors().toSet() == supers.map { it.descriptor }.toSet())
    assert(this.superClasses.isEmpty())
    supers.mapTo(this.superClasses) { it.symbol }

    val superMembers = supers.flatMap {
        it.simpleFunctions()
    }.associateBy { it.descriptor }

    this.simpleFunctions().forEach {
        assert(it.overriddenSymbols.isEmpty())

        it.descriptor.overriddenDescriptors.mapTo(it.overriddenSymbols) {
            val superMember = superMembers[it.original] ?: error(it.original)
            superMember.symbol
        }
    }
}

private fun IrClass.superDescriptors() =
        this.descriptor.typeConstructor.supertypes.map { it.constructor.declarationDescriptor as ClassDescriptor }

fun IrClass.setSuperSymbols(symbolTable: SymbolTable) {
    assert(this.superClasses.isEmpty())
    this.superDescriptors().mapTo(this.superClasses) { symbolTable.referenceClass(it) }
    this.simpleFunctions().forEach {
        it.setOverrides(symbolTable)
    }
    this.typeParameters.forEach {
        it.setSupers(symbolTable)
    }
}

fun IrTypeParameter.setSupers(symbolTable: SymbolTable) {
    assert(this.superClassifiers.isEmpty())
    this.descriptor.upperBounds.mapNotNullTo(this.superClassifiers) {
        it.constructor.declarationDescriptor?.let {
            if (it is TypeParameterDescriptor) {
                IrTypeParameterSymbolImpl(it) // Workaround for deserialized inline functions
            } else {
                symbolTable.referenceClassifier(it)
            }
        }
    }
}

fun IrClass.setSuperSymbolsAndAddFakeOverrides(supers: List<IrClass>) {
    val overriddenSuperMembers = this.declarations.map { it.descriptor }
            .filterIsInstance<CallableMemberDescriptor>().flatMap { it.overriddenDescriptors.map { it.original } }

    val unoverriddenSuperMembers = supers.flatMap {
        it.declarations.mapNotNull {
            when (it) {
                is IrSimpleFunction -> it.descriptor
                is IrProperty -> it.descriptor
                else -> null
            }
        }
    } - overriddenSuperMembers

    val irClass = this

    val overridingStrategy = object : OverridingStrategy() {
        override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
            irClass.addChild(createFakeOverride(fakeOverride, startOffset, endOffset))
        }

        override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
            error("inheritance conflict in synthesized class ${irClass.descriptor}:\n  $first\n  $second")
        }

        override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
            error("override conflict in synthesized class ${irClass.descriptor}:\n  $fromSuper\n  $fromCurrent")
        }
    }

    unoverriddenSuperMembers.groupBy { it.name }.forEach { (name, members) ->
        OverridingUtil.generateOverridesInFunctionGroup(
                name,
                members,
                emptyList(),
                this.descriptor,
                overridingStrategy
        )
    }

    this.setSuperSymbols(supers)
}

private fun IrElement.innerStartOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.startOffset ?: this.startOffset

private fun IrElement.innerEndOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.endOffset ?: this.endOffset

inline fun <reified T> stub(name: String): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) {
        _ /* proxy */, method, _ /* methodArgs */ ->
        if (method.name == "toString" && method.parameterCount == 0) {
            "${T::class.simpleName} stub for $name"
        } else {
            error("${T::class.simpleName}.${method.name} is not supported for $name")
        }
    } as T
}

fun IrDeclarationContainer.addChildren(declarations: List<IrDeclaration>) {
    declarations.forEach { this.addChild(it) }
}

fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    this.declarations += declaration
    declaration.accept(SetDeclarationsParentVisitor, this)
}

object SetDeclarationsParentVisitor : IrElementVisitor<Unit, IrDeclarationParent> {
    override fun visitElement(element: IrElement, data: IrDeclarationParent) {
        if (element !is IrDeclarationParent) {
            element.acceptChildren(this, data)
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent) {
        declaration.parent = data
        super.visitDeclaration(declaration, data)
    }
}

fun IrModuleFragment.checkDeclarationParents() {
    this.accept(CheckDeclarationParentsVisitor, null)
    this.dependencyModules.forEach { dependencyModule ->
        dependencyModule.externalPackageFragments.forEach {
            it.accept(CheckDeclarationParentsVisitor, null)
        }
    }
}

object CheckDeclarationParentsVisitor : IrElementVisitor<Unit, IrDeclarationParent?> {

    override fun visitElement(element: IrElement, data: IrDeclarationParent?) {
        element.acceptChildren(this, element as? IrDeclarationParent ?: data)
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent?) {
        if (declaration !is IrVariable) {
            checkParent(declaration, data)
        } else {
            // Don't check IrVariable parent.
        }

        super.visitDeclaration(declaration, data)
    }

    private fun checkParent(declaration: IrDeclaration, expectedParent: IrDeclarationParent?) {
        val parent = try {
            declaration.parent
        } catch (e: Throwable) {
            error("$declaration for ${declaration.descriptor} has no parent")
        }

        if (parent != expectedParent) {
            error("$declaration for ${declaration.descriptor} has unexpected parent $parent")
        }
    }
}

tailrec fun IrDeclaration.getContainingFile(): IrFile? {
    val parent = this.parent

    return when (parent) {
        is IrFile -> parent
        is IrDeclaration -> parent.getContainingFile()
        else -> null
    }
}

internal fun KonanBackendContext.report(declaration: IrDeclaration, message: String, isError: Boolean) {
    val irFile = declaration.getContainingFile()
    this.report(
            declaration,
            irFile,
            if (irFile != null) {
                message
            } else {
                val renderer = org.jetbrains.kotlin.renderer.DescriptorRenderer.COMPACT_WITH_SHORT_TYPES
                "$message\n${renderer.render(declaration.descriptor)}"
            },
            isError
    )
    if (isError) throw KonanCompilationException()
}
