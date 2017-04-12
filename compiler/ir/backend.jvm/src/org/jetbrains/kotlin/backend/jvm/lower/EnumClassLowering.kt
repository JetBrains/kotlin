/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.lower

import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmPropertyDescriptorImpl
import org.jetbrains.kotlin.backend.jvm.descriptors.createValueParameter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.*
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.*

class EnumClassLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val classDescriptor = irClass.descriptor
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) return

        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private val unsubstitutedArrayOfFun = context.builtIns.findSingleFunction(Name.identifier("arrayOf"))

    private fun createArrayOfExpression(arrayElementType: KotlinType, arrayElements: List<IrExpression>): IrExpression {
        val typeParameter0 = unsubstitutedArrayOfFun.typeParameters[0]
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType)))
        val substitutedArrayOfFun = unsubstitutedArrayOfFun.substitute(typeSubstitutor)!!

        val typeArguments = mapOf(typeParameter0 to arrayElementType)

        val valueParameter0 = substitutedArrayOfFun.valueParameters[0]
        val arg0VarargType = valueParameter0.type
        val arg0VarargElementType = valueParameter0.varargElementType!!
        val arg0 = IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arg0VarargType, arg0VarargElementType, arrayElements)

        return IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, substitutedArrayOfFun, typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val enumEntryOrdinals = TObjectIntHashMap<ClassDescriptor>()
        private val loweredEnumConstructors = HashMap<ClassConstructorDescriptor, ClassConstructorDescriptor>()
        private val loweredEnumConstructorParameters = HashMap<ValueParameterDescriptor, ValueParameterDescriptor>()
        private val enumEntriesByField = HashMap<PropertyDescriptor, ClassDescriptor>()
        private val enumEntryFields = ArrayList<PropertyDescriptor>()

        private lateinit var valuesFieldDescriptor: PropertyDescriptor
        private lateinit var valuesFunctionDescriptor: FunctionDescriptor
        private lateinit var valueOfFunctionDescriptor: FunctionDescriptor

        fun run() {
            assignOrdinalsToEnumEntries()
            lowerEnumConstructors(irClass)
            lowerEnumEntries()
            setupSynthesizedEnumClassMembers()
            lowerEnumClassBody()
        }

        private fun assignOrdinalsToEnumEntries() {
            var ordinal = 0
            irClass.declarations.forEach {
                if (it is IrEnumEntry) {
                    enumEntryOrdinals.put(it.descriptor, ordinal)
                    ordinal++
                }
            }
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.transform { declaration ->
                if (declaration is IrConstructor)
                    transformEnumConstructor(declaration)
                else
                    declaration
            }
        }

        private fun transformEnumConstructor(enumConstructor: IrConstructor): IrConstructor {
            val constructorDescriptor = enumConstructor.descriptor
            val loweredConstructorDescriptor = lowerEnumConstructor(constructorDescriptor)
            val loweredEnumConstructor = IrConstructorImpl(
                    enumConstructor.startOffset, enumConstructor.endOffset, enumConstructor.origin,
                    loweredConstructorDescriptor,
                    enumConstructor.body!! // will be transformed later
            )
            return loweredEnumConstructor
        }

        private fun lowerEnumConstructor(constructorDescriptor: ClassConstructorDescriptor): ClassConstructorDescriptor {
            val loweredConstructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
                    constructorDescriptor.containingDeclaration,
                    constructorDescriptor.annotations,
                    constructorDescriptor.isPrimary,
                    constructorDescriptor.source
            )

            val valueParameters =
                    listOf(
                            loweredConstructorDescriptor.createValueParameter(0, "name", context.builtIns.stringType),
                            loweredConstructorDescriptor.createValueParameter(1, "ordinal", context.builtIns.intType)
                    ) +
                    constructorDescriptor.valueParameters.map {
                        lowerConstructorValueParameter(loweredConstructorDescriptor, it)
                    }
            loweredConstructorDescriptor.initialize(valueParameters, Visibilities.PROTECTED)

            loweredConstructorDescriptor.returnType = constructorDescriptor.returnType

            loweredEnumConstructors[constructorDescriptor] = loweredConstructorDescriptor

            return loweredConstructorDescriptor
        }

        private fun lowerConstructorValueParameter(
                loweredConstructorDescriptor: ClassConstructorDescriptor,
                valueParameterDescriptor: ValueParameterDescriptor
        ): ValueParameterDescriptor {
            val loweredValueParameterDescriptor = valueParameterDescriptor.copy(
                    loweredConstructorDescriptor,
                    valueParameterDescriptor.name,
                    valueParameterDescriptor.index + 2
            )
            loweredEnumConstructorParameters[valueParameterDescriptor] = loweredValueParameterDescriptor
            return loweredValueParameterDescriptor
        }

        private fun lowerEnumEntries() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    listOfNotNull(createFieldForEnumEntry(declaration),
                                  lowerEnumEntryClass(declaration.correspondingClass))
                }
                else null
            }
        }

        private fun lowerEnumEntryClass(enumEntryClass: IrClass?): IrClass? {
            if (enumEntryClass == null) return null

            lowerEnumConstructors(enumEntryClass)

            return enumEntryClass
        }

        private fun createFieldForEnumEntry(enumEntry: IrEnumEntry): IrField {
            val fieldPropertyDescriptor = context.specialDescriptorsFactory.getFieldDescriptorForEnumEntry(enumEntry.descriptor)

            enumEntriesByField[fieldPropertyDescriptor] = enumEntry.descriptor
            enumEntryFields.add(fieldPropertyDescriptor)

            val enumEntryInitializer = enumEntry.initializerExpression!!
            return IrFieldImpl(
                    enumEntry.startOffset, enumEntry.endOffset, JvmLoweredDeclarationOrigin.FIELD_FOR_ENUM_ENTRY,
                    fieldPropertyDescriptor,
                    IrExpressionBodyImpl(enumEntryInitializer)
            )
        }

        private fun setupSynthesizedEnumClassMembers() {
            val irField = createSyntheticValuesFieldDeclaration()

            irClass.declarations.add(irField)

            valuesFunctionDescriptor = findFunctionDescriptorForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUES)
            valueOfFunctionDescriptor = findFunctionDescriptorForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUEOF)
        }

        private fun findFunctionDescriptorForMemberWithSyntheticBodyKind(kind: IrSyntheticBodyKind): FunctionDescriptor =
                irClass.declarations
                        .first {
                            it is IrFunction &&
                            it.body.let { body ->
                                body is IrSyntheticBody && body.kind == kind
                            }
                        }
                        .descriptor as FunctionDescriptor


        private fun createSyntheticValuesFieldDeclaration(): IrFieldImpl {
            val valuesArrayType = context.builtIns.getArrayType(Variance.INVARIANT, irClass.descriptor.defaultType)
            valuesFieldDescriptor = createSyntheticValuesFieldDescriptor(valuesArrayType)

            val irValuesInitializer = createSyntheticValuesFieldInitializerExpression()

            val irField = IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.FIELD_FOR_ENUM_VALUES,
                                      valuesFieldDescriptor,
                                      IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irValuesInitializer))
            return irField
        }

        private fun createSyntheticValuesFieldInitializerExpression(): IrExpression =
                createArrayOfExpression(
                        irClass.descriptor.defaultType,
                        enumEntryFields.map { fieldDescriptor ->
                            IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, fieldDescriptor)
                        })

        private fun createSyntheticValuesFieldDescriptor(valuesArrayType: SimpleType): PropertyDescriptorImpl {
            return JvmPropertyDescriptorImpl.createStaticVal(
                    Name.identifier("\$VALUES"),
                    valuesArrayType,
                    irClass.descriptor,
                    Annotations.EMPTY,
                    Modality.FINAL, Visibilities.PRIVATE, Opcodes.ACC_SYNTHETIC,
                    irClass.descriptor.source
            )
        }

        private fun lowerEnumClassBody() {
            irClass.transformChildrenVoid(EnumClassBodyTransformer())
        }

        private inner class InEnumClassConstructor(val enumClassConstructor: ClassConstructorDescriptor) :
                EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset
                val origin = enumConstructorCall.origin

                val result = IrDelegatingConstructorCallImpl(startOffset, endOffset, enumConstructorCall.descriptor)

                assert(result.descriptor.valueParameters.size == 2) {
                    "Enum(String, Int) constructor call expected:\n${result.dump()}"
                }

                val nameParameter = enumClassConstructor.valueParameters.getOrElse(0) {
                    throw AssertionError("No 'name' parameter in enum constructor: $enumClassConstructor")
                }

                val ordinalParameter = enumClassConstructor.valueParameters.getOrElse(1) {
                    throw AssertionError("No 'ordinal' parameter in enum constructor: $enumClassConstructor")
                }

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, nameParameter, origin))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, ordinalParameter, origin))

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                val descriptor = delegatingConstructorCall.descriptor
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val loweredDelegatedConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = IrDelegatingConstructorCallImpl(startOffset, endOffset, loweredDelegatedConstructor)

                result.putValueArgument(0, IrGetValueImpl(startOffset, endOffset, enumClassConstructor.valueParameters[0]))
                result.putValueArgument(1, IrGetValueImpl(startOffset, endOffset, enumClassConstructor.valueParameters[1]))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, delegatingConstructorCall.getValueArgument(i))
                }

                return result
            }
        }

        private abstract inner class InEnumEntry(private val enumEntry: ClassDescriptor) : EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val name = enumEntry.name.asString()
                val ordinal = enumEntryOrdinals[enumEntry]

                val descriptor = enumConstructorCall.descriptor
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val loweredConstructor = loweredEnumConstructors.getOrElse(descriptor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $descriptor")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor)

                result.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.builtIns.stringType, name))
                result.putValueArgument(1, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, ordinal))

                descriptor.valueParameters.forEach { valueParameter ->
                    val i = valueParameter.index
                    result.putValueArgument(i + 2, enumConstructorCall.getValueArgument(i))
                }

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                throw AssertionError("Unexpected delegating constructor call within enum entry: $enumEntry")
            }

            abstract fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: ClassConstructorDescriptor): IrMemberAccessExpression
        }

        private inner class InEnumEntryClassConstructor(enumEntry: ClassDescriptor) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: ClassConstructorDescriptor) =
                    IrDelegatingConstructorCallImpl(startOffset, endOffset, loweredConstructor)
        }

        private inner class InEnumEntryInitializer(enumEntry: ClassDescriptor) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: ClassConstructorDescriptor) =
                    IrCallImpl(startOffset, endOffset, loweredConstructor)
        }

        private inner class EnumClassBodyTransformer : IrElementTransformerVoid() {
            private var enumConstructorCallTransformer: EnumConstructorCallTransformer? = null

            override fun visitField(declaration: IrField): IrStatement {
                val enumEntry = enumEntriesByField[declaration.descriptor]
                if (enumEntry == null) {
                    declaration.transformChildrenVoid(this)
                    return declaration
                }

                assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}"}

                enumConstructorCallTransformer = InEnumEntryInitializer(enumEntry)

                val result = super.visitField(declaration)

                enumConstructorCallTransformer = null

                return result
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                val constructorDescriptor = declaration.descriptor
                val containingClass = constructorDescriptor.containingDeclaration

                // TODO local (non-enum) class in enum class constructor?
                val previous = enumConstructorCallTransformer

                if (containingClass.kind == ClassKind.ENUM_ENTRY) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}"}
                    enumConstructorCallTransformer = InEnumEntryClassConstructor(containingClass)
                }
                else if (containingClass.kind == ClassKind.ENUM_CLASS) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}"}
                    enumConstructorCallTransformer = InEnumClassConstructor(constructorDescriptor)
                }

                val result = super.visitConstructor(declaration)

                enumConstructorCallTransformer = previous

                return result
            }

            override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val callTransformer = enumConstructorCallTransformer ?:
                                      throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" +
                                                           irClass.dump())

                return callTransformer.transform(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.descriptor.containingDeclaration.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?:
                                          throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" +
                                                               irClass.dump())

                    return callTransformer.transform(expression)
                }

                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val loweredParameter = loweredEnumConstructorParameters[expression.descriptor]
                if (loweredParameter != null) {
                    return IrGetValueImpl(expression.startOffset, expression.endOffset, loweredParameter, expression.origin)
                }
                else {
                    return expression
                }
            }

            override fun visitSyntheticBody(body: IrSyntheticBody): IrBody {
                return when (body.kind) {
                    IrSyntheticBodyKind.ENUM_VALUES ->
                        createEnumValuesBody(valuesFieldDescriptor)
                    IrSyntheticBodyKind.ENUM_VALUEOF ->
                        createEnumValueOfBody()
                    else ->
                        body
                }
            }

            private fun createEnumValueOfBody(): IrBody {
                val unsubstitutedValueOf = context.irBuiltIns.enumValueOf
                val typeParameterT = unsubstitutedValueOf.typeParameters[0]
                val enumClassType = irClass.descriptor.defaultType
                val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(enumClassType)))
                val substitutedValueOf = unsubstitutedValueOf.substitute(typeSubstitutor)!!

                val irValueOfCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, substitutedValueOf, mapOf(typeParameterT to enumClassType))
                irValueOfCall.putValueArgument(
                        0, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueOfFunctionDescriptor.valueParameters[0]))

                return IrBlockBodyImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        listOf(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueOfFunctionDescriptor, irValueOfCall))
                )
            }

            private fun createEnumValuesBody(valuesFieldDescriptor: PropertyDescriptor): IrBody {
                val cloneFun = valuesFieldDescriptor.type.memberScope.findSingleFunction(Name.identifier("clone"))

                val irCloneValues = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, cloneFun).apply {
                    dispatchReceiver = IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesFieldDescriptor)
                }

                return IrBlockBodyImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        listOf(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valuesFunctionDescriptor, irCloneValues))
                )
            }
        }
    }


}