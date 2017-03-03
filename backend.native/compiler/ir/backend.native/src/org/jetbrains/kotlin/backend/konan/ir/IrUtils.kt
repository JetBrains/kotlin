package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.io.StringWriter


fun ir2string(ir: IrElement?): String = ir2stringWhole(ir).takeWhile { it != '\n' }

fun ir2stringWhole(ir: IrElement?): String {
  val strWriter = StringWriter()

  ir?.accept(DumpIrTreeVisitor(strWriter), "")
  return strWriter.toString()
}

internal fun ClassDescriptor.createSimpleDelegatingConstructor(superConstructorDescriptor: ClassConstructorDescriptor)
        : Pair<ClassConstructorDescriptor, IrConstructor> {
    val constructorDescriptor = ClassConstructorDescriptorImpl.createSynthesized(
            this,
            Annotations.EMPTY,
            superConstructorDescriptor.isPrimary,
            SourceElement.NO_SOURCE)
    val valueParameters = superConstructorDescriptor.valueParameters.map {
        it.copy(constructorDescriptor, it.name, it.index)
    }
    constructorDescriptor.initialize(valueParameters, superConstructorDescriptor.visibility)
    constructorDescriptor.returnType = superConstructorDescriptor.returnType

    val body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            listOf(
                    IrDelegatingConstructorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, superConstructorDescriptor).apply {
                        valueParameters.forEachIndexed { idx, parameter ->
                            putValueArgument(idx, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter))
                        }
                    },
                    IrInstanceInitializerCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this)
            )
    )
    val constructor = IrConstructorImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, constructorDescriptor, body)

    return Pair(constructorDescriptor, constructor)
}

internal fun Context.createArrayOfExpression(arrayElementType: KotlinType,
                                             arrayElements: List<IrExpression>): IrExpression {
    val kotlinPackage = irModule!!.descriptor.getPackage(FqName("kotlin"))
    val genericArrayOfFun = kotlinPackage.memberScope.getContributedFunctions(Name.identifier("arrayOf"), NoLookupLocation.FROM_BACKEND).first()
    val typeParameter0 = genericArrayOfFun.typeParameters[0]
    val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameter0.typeConstructor to TypeProjectionImpl(arrayElementType)))
    val substitutedArrayOfFun = genericArrayOfFun.substitute(typeSubstitutor)!!

    val typeArguments = mapOf(typeParameter0 to arrayElementType)

    val valueParameter0 = substitutedArrayOfFun.valueParameters[0]
    val arg0VarargType = valueParameter0.type
    val arg0VarargElementType = valueParameter0.varargElementType!!
    val arg0 = IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arg0VarargType, arg0VarargElementType, arrayElements)

    return IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, substitutedArrayOfFun, typeArguments).apply {
        putValueArgument(0, arg0)
    }
}