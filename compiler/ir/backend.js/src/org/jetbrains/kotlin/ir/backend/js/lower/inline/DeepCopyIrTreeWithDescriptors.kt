/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.IrElementVisitorVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.createClassSymbolOrNull
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance


// backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/DeepCopyIrTreeWithDescriptors.kt
internal class DeepCopyIrTreeWithDescriptors(val targetDescriptor: FunctionDescriptor,
                                             val parentDescriptor: DeclarationDescriptor,
                                             val context: JsIrBackendContext) {

    private val descriptorSubstituteMap: MutableMap<DeclarationDescriptor, DeclarationDescriptor> = mutableMapOf()
    private var typeSubstitutor: TypeSubstitutor? = null
    private var nameIndex = 0

    //-------------------------------------------------------------------------//

    fun copy(irElement: IrElement, typeSubstitutor: TypeSubstitutor?): IrElement {
        this.typeSubstitutor = typeSubstitutor
        // Create all class descriptors and all necessary descriptors in order to create KotlinTypes.
        irElement.acceptChildrenVoid(DescriptorCollectorCreatePhase())
        // Initialize all created descriptors possibly using previously created types.
        irElement.acceptChildrenVoid(DescriptorCollectorInitPhase())
        return irElement.accept(InlineCopyIr(), null)
    }

    inner class DescriptorCollectorCreatePhase : IrElementVisitorVoidWithContext() {

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitClassNew(declaration: IrClass) {
            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyClassDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            descriptorSubstituteMap[oldDescriptor.thisAsReceiverParameter] = newDescriptor.thisAsReceiverParameter

            super.visitClassNew(declaration)

            val constructors = oldDescriptor.constructors.map { oldConstructorDescriptor ->
                descriptorSubstituteMap[oldConstructorDescriptor] as ClassConstructorDescriptor
            }.toSet()

            val oldPrimaryConstructor = oldDescriptor.unsubstitutedPrimaryConstructor
            val primaryConstructor = oldPrimaryConstructor?.let { descriptorSubstituteMap[it] as ClassConstructorDescriptor }

            val contributedDescriptors = oldDescriptor.unsubstitutedMemberScope
                .getContributedDescriptors()
                .map { descriptorSubstituteMap[it]!! }
            newDescriptor.initialize(
                SimpleMemberScope(contributedDescriptors),
                constructors,
                primaryConstructor
            )
        }

        //---------------------------------------------------------------------//

        override fun visitPropertyNew(declaration: IrProperty) {
            copyPropertyOrField(declaration.descriptor)
            super.visitPropertyNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFieldNew(declaration: IrField) {
            val oldDescriptor = declaration.descriptor
            if (descriptorSubstituteMap[oldDescriptor] == null) {
                copyPropertyOrField(oldDescriptor)                                          // A field without a property or a field of a delegated property.
            }
            super.visitFieldNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunctionNew(declaration: IrFunction) {
            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) {                             // Property accessors are copied along with their property.
                val oldContainingDeclaration =
                    if (oldDescriptor.visibility == Visibilities.LOCAL)
                        parentDescriptor
                    else
                        oldDescriptor.containingDeclaration
                descriptorSubstituteMap[oldDescriptor] = copyFunctionDescriptor(oldDescriptor, oldContainingDeclaration)
            }
            super.visitFunctionNew(declaration)
        }

        //--- Copy descriptors ------------------------------------------------//

        private fun generateCopyName(name: Name): Name {
            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(declarationName /*+ "_" + indexStr*/)
        }

        //---------------------------------------------------------------------//

        private fun copyFunctionDescriptor(oldDescriptor: CallableDescriptor, oldContainingDeclaration: DeclarationDescriptor) =
            when (oldDescriptor) {
                is ConstructorDescriptor -> copyConstructorDescriptor(oldDescriptor)
                is SimpleFunctionDescriptor -> copySimpleFunctionDescriptor(oldDescriptor, oldContainingDeclaration)
                else -> TODO("Unsupported FunctionDescriptor subtype: $oldDescriptor")
            }

        //---------------------------------------------------------------------//

        private fun copySimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor, oldContainingDeclaration: DeclarationDescriptor) : FunctionDescriptor {
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration,
                /* annotations           = */ oldDescriptor.annotations,
                /* name                  = */ generateCopyName(oldDescriptor.name),
                /* kind                  = */ oldDescriptor.kind,
                /* source                = */ oldDescriptor.source
            )
        }

        //---------------------------------------------------------------------//

        private fun copyConstructorDescriptor(oldDescriptor: ConstructorDescriptor) : FunctionDescriptor {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return ClassConstructorDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration as ClassDescriptor,
                /* annotations           = */ oldDescriptor.annotations,
                /* isPrimary             = */ oldDescriptor.isPrimary,
                /* source                = */ oldDescriptor.source
            )
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyOrField(oldDescriptor: PropertyDescriptor) {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration) as ClassDescriptor
            @Suppress("DEPRECATION")
            val newDescriptor = PropertyDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isVar                 = */ oldDescriptor.isVar,
                /* name                  = */ oldDescriptor.name,
                /* kind                  = */ oldDescriptor.kind,
                /* source                = */ oldDescriptor.source,
                /* lateInit              = */ oldDescriptor.isLateInit,
                /* isConst               = */ oldDescriptor.isConst,
                /* isExpect              = */ oldDescriptor.isExpect,
                /* isActual              = */ oldDescriptor.isActual,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isDelegated           = */ oldDescriptor.isDelegated
            )
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
        }

        //---------------------------------------------------------------------//

        private fun copyClassDescriptor(oldDescriptor: ClassDescriptor): ClassDescriptorImpl {
            val oldSuperClass = oldDescriptor.getSuperClassOrAny()
            val newSuperClass = descriptorSubstituteMap.getOrDefault(oldSuperClass, oldSuperClass) as ClassDescriptor
            val oldInterfaces = oldDescriptor.getSuperInterfaces()
            val newInterfaces = oldInterfaces.map { descriptorSubstituteMap.getOrDefault(it, it) as ClassDescriptor }
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            val newName = if (DescriptorUtils.isAnonymousObject(oldDescriptor))      // Anonymous objects are identified by their name.
                oldDescriptor.name                                     // We need to preserve it for LocalDeclarationsLowering.
            else
                generateCopyName(oldDescriptor.name)

            val visibility = oldDescriptor.visibility

            return object : ClassDescriptorImpl(
                /* containingDeclaration = */ newContainingDeclaration,
                /* name                  = */ newName,
                /* modality              = */ oldDescriptor.modality,
                /* kind                  = */ oldDescriptor.kind,
                /* supertypes            = */ listOf(newSuperClass.defaultType) + newInterfaces.map { it.defaultType },
                /* source                = */ oldDescriptor.source,
                /* isExternal            = */ oldDescriptor.isExternal,
                                              LockBasedStorageManager.NO_LOCKS
            ) {
                override fun getVisibility() = visibility
            }
        }
    }

    //-------------------------------------------------------------------------//

    inner class DescriptorCollectorInitPhase : IrElementVisitorVoidWithContext() {

        private val initializedProperties = mutableSetOf<PropertyDescriptor>()

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitPropertyNew(declaration: IrProperty) {
            initPropertyOrField(declaration.descriptor)
            super.visitPropertyNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFieldNew(declaration: IrField) {
            val oldDescriptor = declaration.descriptor
            if (!initializedProperties.contains(oldDescriptor)) {
                initPropertyOrField(oldDescriptor)                                          // A field without a property or a field of a delegated property.
            }
            super.visitFieldNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunctionNew(declaration: IrFunction) {
            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) {                             // Property accessors are copied along with their property.
                val newDescriptor = initFunctionDescriptor(oldDescriptor)
                oldDescriptor.extensionReceiverParameter?.let {
                    descriptorSubstituteMap[it] = newDescriptor.extensionReceiverParameter!!
                }
            }
            super.visitFunctionNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable) {
            declaration.descriptor.let { descriptorSubstituteMap[it] = copyVariableDescriptor(it) }

            super.visitVariable(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitCatch(aCatch: IrCatch) {
            aCatch.parameter.let { descriptorSubstituteMap[it] = copyVariableDescriptor(it) }

            super.visitCatch(aCatch)
        }

        //--- Copy descriptors ------------------------------------------------//

        private fun generateCopyName(name: Name): Name {
            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(declarationName /*+ "_" + indexStr*/)
        }

        //---------------------------------------------------------------------//

        private fun copyVariableDescriptor(oldDescriptor: VariableDescriptor): VariableDescriptor {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return IrTemporaryVariableDescriptorImpl(
                containingDeclaration = newContainingDeclaration,
                name                  = generateCopyName(oldDescriptor.name),
                outType               = substituteTypeAndTryGetCopied(oldDescriptor.type)!!,
                isMutable             = oldDescriptor.isVar
            )
        }

        //---------------------------------------------------------------------//

        private fun initFunctionDescriptor(oldDescriptor: CallableDescriptor): CallableDescriptor =
            when (oldDescriptor) {
                is ConstructorDescriptor -> initConstructorDescriptor(oldDescriptor)
                is SimpleFunctionDescriptor -> initSimpleFunctionDescriptor(oldDescriptor)
                else -> TODO("Unsupported FunctionDescriptor subtype: $oldDescriptor")
            }

        //---------------------------------------------------------------------//

        private fun initSimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor): FunctionDescriptor =
            (descriptorSubstituteMap[oldDescriptor] as SimpleFunctionDescriptorImpl).apply {
                val oldDispatchReceiverParameter = oldDescriptor.dispatchReceiverParameter
                val newDispatchReceiverParameter = oldDispatchReceiverParameter?.let { descriptorSubstituteMap.getOrDefault(it, it) as ReceiverParameterDescriptor }
                val newTypeParameters = oldDescriptor.typeParameters        // TODO substitute types
                val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, this)
                val newReceiverParameter = copyReceiverParameter(oldDescriptor.extensionReceiverParameter, this)
                val newReturnType = substituteTypeAndTryGetCopied(oldDescriptor.returnType)

                initialize(
                    /* extensionReceiverParameter   = */ newReceiverParameter,
                    /* dispatchReceiverParameter    = */ newDispatchReceiverParameter,
                    /* typeParameters               = */ newTypeParameters,
                    /* unsubstitutedValueParameters = */ newValueParameters,
                    /* unsubstitutedReturnType      = */ newReturnType,
                    /* modality                     = */ oldDescriptor.modality,
                    /* visibility                   = */ oldDescriptor.visibility
                )
                isTailrec = oldDescriptor.isTailrec
                isSuspend = oldDescriptor.isSuspend
                overriddenDescriptors += oldDescriptor.overriddenDescriptors
            }

        //---------------------------------------------------------------------//

        private fun initConstructorDescriptor(oldDescriptor: ConstructorDescriptor): FunctionDescriptor =
            (descriptorSubstituteMap[oldDescriptor] as ClassConstructorDescriptorImpl).apply {
                val newTypeParameters = oldDescriptor.typeParameters
                val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, this)
                val newReceiverParameter = copyReceiverParameter(oldDescriptor.dispatchReceiverParameter, this)
                val returnType = substituteTypeAndTryGetCopied(oldDescriptor.returnType)

                initialize(
                    /* extensionReceiverParameter   = */ newReceiverParameter,
                    /* dispatchReceiverParameter    = */ null,                              //  For constructor there is no explicit dispatch receiver.
                    /* typeParameters               = */ newTypeParameters,
                    /* unsubstitutedValueParameters = */ newValueParameters,
                    /* unsubstitutedReturnType      = */ returnType,
                    /* modality                     = */ oldDescriptor.modality,
                    /* visibility                   = */ oldDescriptor.visibility
                )
            }

        //---------------------------------------------------------------------//

        private fun initPropertyOrField(oldDescriptor: PropertyDescriptor) {
            val newDescriptor = (descriptorSubstituteMap[oldDescriptor] as PropertyDescriptorImpl).apply {
                setType(
                    /* outType                   = */ substituteTypeAndTryGetCopied(oldDescriptor.type)!!,
                    /* typeParameters            = */ oldDescriptor.typeParameters,
                    /* dispatchReceiverParameter = */ (containingDeclaration as ClassDescriptor).thisAsReceiverParameter,
                    /* receiverType              = */ substituteTypeAndTryGetCopied(oldDescriptor.extensionReceiverParameter?.type))

                initialize(
                    /* getter = */ oldDescriptor.getter?.let { copyPropertyGetterDescriptor(it, this) },
                    /* setter = */ oldDescriptor.setter?.let { copyPropertySetterDescriptor(it, this) })

                overriddenDescriptors += oldDescriptor.overriddenDescriptors
            }
            oldDescriptor.getter?.let { descriptorSubstituteMap[it] = newDescriptor.getter!! }
            oldDescriptor.setter?.let { descriptorSubstituteMap[it] = newDescriptor.setter!! }
            oldDescriptor.extensionReceiverParameter?.let {
                descriptorSubstituteMap[it] = newDescriptor.extensionReceiverParameter!!
            }
            initializedProperties.add(oldDescriptor)
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyGetterDescriptor(oldDescriptor: PropertyGetterDescriptor, newPropertyDescriptor: PropertyDescriptor) =
            PropertyGetterDescriptorImpl(
                /* correspondingProperty = */ newPropertyDescriptor,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isDefault             = */ oldDescriptor.isDefault,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isInline              = */ oldDescriptor.isInline,
                /* kind                  = */ oldDescriptor.kind,
                /* original              = */ null,
                /* source                = */ oldDescriptor.source).apply {
                initialize(substituteTypeAndTryGetCopied(oldDescriptor.returnType))
            }

        //---------------------------------------------------------------------//

        private fun copyPropertySetterDescriptor(oldDescriptor: PropertySetterDescriptor, newPropertyDescriptor: PropertyDescriptor) =
            PropertySetterDescriptorImpl(
                /* correspondingProperty = */ newPropertyDescriptor,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isDefault             = */ oldDescriptor.isDefault,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isInline              = */ oldDescriptor.isInline,
                /* kind                  = */ oldDescriptor.kind,
                /* original              = */ null,
                /* source                = */ oldDescriptor.source).apply {
                initialize(copyValueParameters(oldDescriptor.valueParameters, this).single())
            }

        //-------------------------------------------------------------------------//

        private fun copyValueParameters(oldValueParameters: List <ValueParameterDescriptor>, containingDeclaration: CallableDescriptor) =
            oldValueParameters.map { oldDescriptor ->
                val newDescriptor = ValueParameterDescriptorImpl(
                    containingDeclaration = containingDeclaration,
                    original              = oldDescriptor.original,
                    index                 = oldDescriptor.index,
                    annotations           = oldDescriptor.annotations,
                    name                  = oldDescriptor.name,
                    outType               = substituteTypeAndTryGetCopied(oldDescriptor.type)!!,
                    declaresDefaultValue  = oldDescriptor.declaresDefaultValue(),
                    isCrossinline         = oldDescriptor.isCrossinline,
                    isNoinline            = oldDescriptor.isNoinline,
                    varargElementType     = substituteTypeAndTryGetCopied(oldDescriptor.varargElementType),
                    source                = oldDescriptor.source
                )
                descriptorSubstituteMap[oldDescriptor] = newDescriptor
                newDescriptor
            }

        private fun copyReceiverParameter(
            oldReceiverParameter: ReceiverParameterDescriptor?, containingDeclaration: CallableDescriptor
        ): ReceiverParameterDescriptor? {
            if (oldReceiverParameter == null) return null
            val substituteTypeAndTryGetCopied = substituteTypeAndTryGetCopied(oldReceiverParameter.type) ?: return null
            return ReceiverParameterDescriptorImpl(
                containingDeclaration,
                ExtensionReceiver(containingDeclaration, substituteTypeAndTryGetCopied, oldReceiverParameter.value),
                oldReceiverParameter.annotations
            )
        }

        private fun substituteTypeAndTryGetCopied(type: KotlinType?): KotlinType? {
            val substitutedType = substituteType(type) ?: return null
            val oldClassDescriptor = TypeUtils.getClassDescriptor(substitutedType) ?: return substitutedType
            return descriptorSubstituteMap[oldClassDescriptor]?.let { (it as ClassDescriptor).defaultType } ?: substitutedType
        }
    }

//-----------------------------------------------------------------------------//

    @Suppress("DEPRECATION")
    inner class InlineCopyIr : DeepCopyIrTree() {

        override fun mapClassDeclaration            (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapTypeAliasDeclaration        (descriptor: TypeAliasDescriptor)             = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as TypeAliasDescriptor
        override fun mapFunctionDeclaration         (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapConstructorDeclaration      (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapPropertyDeclaration         (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapLocalPropertyDeclaration    (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapEnumEntryDeclaration        (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapVariableDeclaration         (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapErrorDeclaration            (descriptor: DeclarationDescriptor)           = descriptorSubstituteMap.getOrDefault(descriptor, descriptor)

        override fun mapClassReference              (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapValueReference              (descriptor: ValueDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ValueDescriptor
        override fun mapVariableReference           (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapPropertyReference           (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapCallee                      (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapDelegatedConstructorCallee  (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapEnumConstructorCallee       (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapLocalPropertyReference      (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapClassifierReference         (descriptor: ClassifierDescriptor)            = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassifierDescriptor
        override fun mapReturnTarget                (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor

        //---------------------------------------------------------------------//

        override fun mapSuperQualifier(qualifier: ClassDescriptor?): ClassDescriptor? {
            if (qualifier == null) return null
            return descriptorSubstituteMap.getOrDefault(qualifier,  qualifier) as ClassDescriptor
        }

        //--- Visits ----------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrCall {
            if (expression !is IrCallImpl) return super.visitCall(expression)
            val newDescriptor = mapCallee(expression.descriptor)
            return IrCallImpl(
                startOffset    = expression.startOffset,
                endOffset      = expression.endOffset,
                type           = newDescriptor.returnType?.toIrType()!!,
                descriptor     = newDescriptor,
                typeArgumentsCount = expression.typeArgumentsCount,
                origin         = expression.origin,
                superQualifierDescriptor = mapSuperQualifier(expression.superQualifier)
            ).apply {
                transformValueArguments(expression)
                substituteTypeArguments(expression)
            }
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction): IrFunction =
            IrFunctionImpl(
                startOffset = declaration.startOffset,
                endOffset   = declaration.endOffset,
                origin      = mapDeclarationOrigin(declaration.origin),
                descriptor  = mapFunctionDeclaration(declaration.descriptor),
                body        = declaration.body?.transform(this, null)
            ).also {
                it.setOverrides(context.symbolTable)
            }.transformParameters(declaration)

        //---------------------------------------------------------------------//

        private fun <T : IrFunction> T.transformDefaults(original: T): T {
            for (originalValueParameter in original.descriptor.valueParameters) {
                val valueParameter = descriptor.valueParameters[originalValueParameter.index]
                original.getDefault(originalValueParameter)?.let { irDefaultParameterValue ->
                    putDefault(valueParameter, irDefaultParameterValue.transform(this@InlineCopyIr, null))
                }
            }
            return this
        }

        //---------------------------------------------------------------------//

        fun getTypeOperatorReturnType(operator: IrTypeOperator, type: IrType) : IrType {
            return when (operator) {
                IrTypeOperator.CAST,
                IrTypeOperator.IMPLICIT_CAST,
                IrTypeOperator.IMPLICIT_NOTNULL,
                IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                IrTypeOperator.IMPLICIT_INTEGER_COERCION    -> type
                IrTypeOperator.SAFE_CAST                    -> type.makeNullable()
                IrTypeOperator.INSTANCEOF,
                IrTypeOperator.NOT_INSTANCEOF               -> context.irBuiltIns.booleanType
            }
        }

        //---------------------------------------------------------------------//

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall {
            val typeOperand = substituteType(expression.typeOperand)!!
            val returnType = getTypeOperatorReturnType(expression.operator, typeOperand)
            return IrTypeOperatorCallImpl(
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = returnType,
                operator    = expression.operator,
                typeOperand = typeOperand,
                argument    = expression.argument.transform(this, null),
                typeOperandClassifier = typeOperand.classifierOrFail
            )
        }

        //---------------------------------------------------------------------//

        override fun visitReturn(expression: IrReturn): IrReturn =
            IrReturnImpl(
                startOffset  = expression.startOffset,
                endOffset    = expression.endOffset,
                type         = substituteType(expression.type)!!,
                returnTargetDescriptor = mapReturnTarget(expression.returnTarget),
                value        = expression.value.transform(this, null)
            )

        //---------------------------------------------------------------------//

        override fun visitBlock(expression: IrBlock): IrBlock {
            return if (expression is IrReturnableBlock) {
                IrReturnableBlockImpl(
                    startOffset    = expression.startOffset,
                    endOffset      = expression.endOffset,
                    type           = expression.type,
                    descriptor     = expression.descriptor,
                    origin         = mapStatementOrigin(expression.origin),
                    statements     = expression.statements.map { it.transform(this, null) },
                    sourceFileName = expression.sourceFileName
                )
            } else {
                super.visitBlock(expression)
            }
        }

        //-------------------------------------------------------------------------//

        override fun visitClassReference(expression: IrClassReference): IrClassReference {
            val newExpressionType = substituteType(expression.type)!!                       // Substituted expression type.
            val newDescriptorType = substituteType(expression.descriptor.defaultType)!!     // Substituted type of referenced class.
            val classDescriptor = newDescriptorType.constructor.declarationDescriptor!!     // Get ClassifierDescriptor of the referenced class.
            return IrClassReferenceImpl(
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = newExpressionType,
                descriptor  = classDescriptor,
                classType   = expression.classType
            )
        }

        //-------------------------------------------------------------------------//

        override fun visitGetClass(expression: IrGetClass): IrGetClass {
            val type = substituteType(expression.type)!!
            return IrGetClassImpl(
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = type,
                argument    = expression.argument.transform(this, null)
            )
        }

        //-------------------------------------------------------------------------//

        override fun getNonTransformedLoop(irLoop: IrLoop): IrLoop {
            return irLoop
        }
    }

    //-------------------------------------------------------------------------//

    private fun substituteType(oldType: IrType?): IrType? = substituteType(oldType?.toKotlinType())?.toIrType()

    private fun substituteType(oldType: KotlinType?): KotlinType? {
        if (typeSubstitutor == null) return oldType
        if (oldType == null)         return oldType
        return typeSubstitutor!!.substitute(oldType, Variance.INVARIANT) ?: oldType
    }

    //-------------------------------------------------------------------------//

    private fun IrMemberAccessExpression.substituteTypeArguments(original: IrMemberAccessExpression) {
        for (index in 0 until original.typeArgumentsCount) {
            val originalTypeArgument = original.getTypeArgument(index)
            val newTypeArgument = substituteType(originalTypeArgument)!!
            this.putTypeArgument(index, newTypeArgument)
        }
    }

    //-------------------------------------------------------------------------//

    fun addCurrentSubstituteMap(globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>) {
        descriptorSubstituteMap.forEach { t, u ->
            globalSubstituteMap.put(t, SubstitutedDescriptor(targetDescriptor, u))
        }
    }

}

class SubstitutedDescriptor(val inlinedFunction: FunctionDescriptor, val descriptor: DeclarationDescriptor)

class DescriptorSubstitutorForExternalScope(val globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>)
    : IrElementTransformerVoidWithContext() {

    private val variableSubstituteMap = mutableMapOf<VariableDescriptor, VariableDescriptor>()

    fun run(element: IrElement) {
        collectVariables(element)
        element.transformChildrenVoid(this)
    }

    private fun collectVariables(element: IrElement) {
        element.acceptChildrenVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                declaration.acceptChildrenVoid(this)

                val oldDescriptor = declaration.descriptor
                val oldClassDescriptor = oldDescriptor.type.constructor.declarationDescriptor as? ClassDescriptor
                val substitutedDescriptor = oldClassDescriptor?.let { globalSubstituteMap[it] }
                if (substitutedDescriptor == null || allScopes.any { it.scope.scopeOwner == substitutedDescriptor.inlinedFunction })
                    return
                val newDescriptor = IrTemporaryVariableDescriptorImpl(
                    containingDeclaration = oldDescriptor.containingDeclaration,
                    name                  = oldDescriptor.name,
                    outType               = (substitutedDescriptor.descriptor as ClassDescriptor).defaultType,
                    isMutable             = oldDescriptor.isVar)
                variableSubstituteMap[oldDescriptor] = newDescriptor
            }
        })
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val oldExpression = super.visitCall(expression) as IrCall

        val substitutedDescriptor = globalSubstituteMap[expression.descriptor.original]
                ?: return oldExpression
        if (allScopes.any { it.scope.scopeOwner == substitutedDescriptor.inlinedFunction })
            return oldExpression
        return when (oldExpression) {
            is IrCallImpl -> copyIrCallImpl(oldExpression, substitutedDescriptor)
            is IrCallWithShallowCopy -> copyIrCallWithShallowCopy(oldExpression, substitutedDescriptor)
            else -> oldExpression
        }
    }

    //---------------------------------------------------------------------//

    override fun visitVariable(declaration: IrVariable): IrDeclaration {
        declaration.transformChildrenVoid(this)

        val oldDescriptor = declaration.descriptor
        val newDescriptor = variableSubstituteMap[oldDescriptor] ?: return declaration

        return IrVariableImpl(
            startOffset = declaration.startOffset,
            endOffset   = declaration.endOffset,
            origin      = declaration.origin,
            descriptor  = newDescriptor,
            initializer = declaration.initializer,
            type        = declaration.type
        )
    }

    //-------------------------------------------------------------------------//

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        expression.transformChildrenVoid(this)

        val oldDescriptor = expression.descriptor
        val newDescriptor = variableSubstituteMap[oldDescriptor] ?: return expression

        return IrGetValueImpl(
            startOffset = expression.startOffset,
            endOffset   = expression.endOffset,
            origin      = expression.origin,
            symbol      = createValueSymbol(newDescriptor),
            type        = expression.type
        )
    }

    //-------------------------------------------------------------------------//

    private fun copyIrCallImpl(oldExpression: IrCallImpl, substitutedDescriptor: SubstitutedDescriptor): IrCallImpl {

        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = substitutedDescriptor.descriptor as FunctionDescriptor

        if (newDescriptor == oldDescriptor)
            return oldExpression

        return IrCallImpl(
            startOffset              = oldExpression.startOffset,
            endOffset                = oldExpression.endOffset,
            type                     = oldExpression.type,
            symbol                   = createFunctionSymbol(newDescriptor),
            descriptor               = newDescriptor,
            typeArgumentsCount       = oldExpression.typeArgumentsCount,
            origin                   = oldExpression.origin,
            superQualifierSymbol     = createClassSymbolOrNull(oldExpression.superQualifier)
        ).apply {
            copyTypeArgumentsFrom(oldExpression)

            oldExpression.descriptor.valueParameters.forEach {
                val valueArgument = oldExpression.getValueArgument(it)
                putValueArgument(it.index, valueArgument)
            }
            extensionReceiver = oldExpression.extensionReceiver
            dispatchReceiver  = oldExpression.dispatchReceiver
        }
    }

    //-------------------------------------------------------------------------//

    private fun copyIrCallWithShallowCopy(oldExpression: IrCallWithShallowCopy, substitutedDescriptor: SubstitutedDescriptor): IrCall {

        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = substitutedDescriptor.descriptor as FunctionDescriptor

        if (newDescriptor == oldDescriptor)
            return oldExpression

        return oldExpression.shallowCopy(oldExpression.origin, createFunctionSymbol(newDescriptor), oldExpression.superQualifierSymbol)
    }
}
