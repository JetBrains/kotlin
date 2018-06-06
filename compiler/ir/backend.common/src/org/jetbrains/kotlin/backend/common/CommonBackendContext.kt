package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.builtins.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.reflect.KProperty


interface CommonBackendContext : BackendContext {

    val ir: Ir<CommonBackendContext>

    //TODO move to builtins
    fun getInternalClass(name: String): ClassDescriptor

    fun getClass(fqName: FqName): ClassDescriptor

    //TODO move to builtins
    fun getInternalFunctions(name: String): List<FunctionDescriptor>

    val reflectionTypes: ReflectionTypes

    fun log(message: () -> String)

    fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean)
}

class ReflectionTypes(module: ModuleDescriptor, internalPackage: FqName) {

    private val kotlinReflectScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }

    private val internalScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(internalPackage).memberScope
    }

    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_REFLECTION) as ClassDescriptor
    }

    private class ClassLookup(val memberScope: MemberScope) {
        operator fun getValue(types: ReflectionTypes, property: KProperty<*>): ClassDescriptor {
            return types.find(memberScope, property.name.capitalize())
        }
    }

    private fun getFunctionTypeArgumentProjections(
            receiverType: KotlinType?,
            parameterTypes: List<KotlinType>,
            returnType: KotlinType
    ): List<TypeProjection> {
        val arguments = ArrayList<TypeProjection>(parameterTypes.size + (if (receiverType != null) 1 else 0) + 1)

        arguments.addIfNotNull(receiverType?.asTypeProjection())

        parameterTypes.mapTo(arguments, KotlinType::asTypeProjection)

        arguments.add(returnType.asTypeProjection())

        return arguments
    }

    fun getKFunction(n: Int): ClassDescriptor = find(kotlinReflectScope, "KFunction$n")

    val kClass: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty0: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty1: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty2: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty0: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty1: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty2: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kFunctionImpl: ClassDescriptor by ClassLookup(internalScope)
    val kProperty0Impl: ClassDescriptor by ClassLookup(internalScope)
    val kProperty1Impl: ClassDescriptor by ClassLookup(internalScope)
    val kProperty2Impl: ClassDescriptor by ClassLookup(internalScope)
    val kMutableProperty0Impl: ClassDescriptor by ClassLookup(internalScope)
    val kMutableProperty1Impl: ClassDescriptor by ClassLookup(internalScope)
    val kMutableProperty2Impl: ClassDescriptor by ClassLookup(internalScope)
    val kLocalDelegatedPropertyImpl: ClassDescriptor by ClassLookup(internalScope)
    val kLocalDelegatedMutablePropertyImpl: ClassDescriptor by ClassLookup(internalScope)

    fun getKFunctionType(
            annotations: Annotations,
            receiverType: KotlinType?,
            parameterTypes: List<KotlinType>,
            returnType: KotlinType
    ): KotlinType {
        val arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType)
        val classDescriptor = getKFunction(arguments.size - 1 /* return type */)
        return KotlinTypeFactory.simpleNotNullType(annotations, classDescriptor, arguments)
    }
}