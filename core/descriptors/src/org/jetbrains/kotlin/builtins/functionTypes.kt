/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_NAME
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.BuiltInAnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

private fun KotlinType.isTypeOrSubtypeOf(predicate: (KotlinType) -> Boolean): Boolean =
        predicate(this) ||
        DFS.dfsFromNode(
                this,
                DFS.Neighbors { it.constructor.supertypes },
                DFS.VisitedWithSet(),
                object : DFS.AbstractNodeHandler<KotlinType, Boolean>() {
                    private var result = false

                    override fun beforeChildren(current: KotlinType): Boolean {
                        if (predicate(current)) {
                            result = true
                        }
                        return !result
                    }

                    override fun result() = result
                }
        )

val KotlinType.isFunctionTypeOrSubtype: Boolean
    get() = isTypeOrSubtypeOf { it.isFunctionType }

val KotlinType.isSuspendFunctionTypeOrSubtype: Boolean
    get() = isTypeOrSubtypeOf { it.isSuspendFunctionType }

val KotlinType.isBuiltinFunctionalTypeOrSubtype: Boolean
    get() = isTypeOrSubtypeOf { it.isBuiltinFunctionalType }

fun KotlinType.isFunctionTypeOrSubtype(predicate: (KotlinType) -> Boolean): Boolean =
    isTypeOrSubtypeOf { it.isFunctionType && predicate(it) }

val KotlinType.isFunctionType: Boolean
    get() = constructor.declarationDescriptor?.getFunctionalClassKind() == FunctionClassKind.Function

val KotlinType.isKFunctionType: Boolean
    get() = constructor.declarationDescriptor?.getFunctionalClassKind() == FunctionClassKind.KFunction

val KotlinType.isSuspendFunctionType: Boolean
    get() = constructor.declarationDescriptor?.getFunctionalClassKind() == FunctionClassKind.SuspendFunction

val KotlinType.isKSuspendFunctionType: Boolean
    get() = constructor.declarationDescriptor?.getFunctionalClassKind() == FunctionClassKind.KSuspendFunction

val KotlinType.isFunctionOrSuspendFunctionType: Boolean
    get() = isFunctionType || isSuspendFunctionType

val KotlinType.isFunctionOrKFunctionTypeWithAnySuspendability: Boolean
    get() = isFunctionType || isSuspendFunctionType || isKFunctionType || isKSuspendFunctionType

val KotlinType.isBuiltinFunctionalType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinFunctionalClassDescriptor == true

val DeclarationDescriptor.isBuiltinFunctionalClassDescriptor: Boolean
    get() {
        val functionalClassKind = getFunctionalClassKind()
        return functionalClassKind == FunctionClassKind.Function ||
                functionalClassKind == FunctionClassKind.SuspendFunction
    }

fun isBuiltinFunctionClass(classId: ClassId): Boolean {
    if (!classId.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) return false

    val kind = classId.asSingleFqName().toUnsafe().getFunctionalClassKind()
    return kind == FunctionClassKind.Function ||
           kind == FunctionClassKind.SuspendFunction
}

val KotlinType.isNonExtensionFunctionType: Boolean
    get() = isFunctionType && !isTypeAnnotatedWithExtensionFunctionType

val KotlinType.isExtensionFunctionType: Boolean
    get() = isFunctionType && isTypeAnnotatedWithExtensionFunctionType

val KotlinType.isBuiltinExtensionFunctionalType: Boolean
    get() = isBuiltinFunctionalType && isTypeAnnotatedWithExtensionFunctionType

private val KotlinType.isTypeAnnotatedWithExtensionFunctionType: Boolean
    get() = annotations.findAnnotation(StandardNames.FqNames.extensionFunctionType) != null

/**
 * @return true if this is an FQ name of a fictitious class representing the function type,
 * e.g. kotlin.Function1 (but NOT kotlin.reflect.KFunction1)
 */
fun isNumberedFunctionClassFqName(fqName: FqNameUnsafe): Boolean {
    return fqName.startsWith(BUILT_INS_PACKAGE_NAME) &&
           fqName.getFunctionalClassKind() == FunctionClassKind.Function
}

fun DeclarationDescriptor.getFunctionalClassKind(): FunctionClassKind? {
    if (this !is ClassDescriptor) return null
    if (!KotlinBuiltIns.isUnderKotlinPackage(this)) return null

    return fqNameUnsafe.getFunctionalClassKind()
}

private fun FqNameUnsafe.getFunctionalClassKind(): FunctionClassKind? {
    if (!isSafe || isRoot) return null

    return FunctionClassKind.getFunctionalClassKind(shortName().asString(), toSafe().parent())
}

fun KotlinType.contextFunctionTypeParamsCount(): Int {
    val annotationDescriptor = annotations.findAnnotation(StandardNames.FqNames.contextFunctionTypeParams) ?: return 0
    val constantValue = annotationDescriptor.allValueArguments.getValue(Name.identifier("count"))
    return (constantValue as IntValue).value
}

fun KotlinType.getReceiverTypeFromFunctionType(): KotlinType? {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    if (!isTypeAnnotatedWithExtensionFunctionType) {
        return null
    }
    val index = contextFunctionTypeParamsCount()
    return arguments[index].type
}

fun KotlinType.getContextReceiverTypesFromFunctionType(): List<KotlinType> {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    val contextReceiversCount = contextFunctionTypeParamsCount()
    return if (contextReceiversCount == 0) {
        emptyList()
    } else {
        arguments.subList(0, contextReceiversCount).map { it.type }
    }
}

fun KotlinType.getReturnTypeFromFunctionType(): KotlinType {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    return arguments.last().type
}

fun KotlinType.replaceReturnType(newReturnType: KotlinType): KotlinType {
    assert(isBuiltinFunctionalType) { "Not a function type: $this"}
    val argumentsWithNewReturnType = arguments.toMutableList().apply { set(size - 1, TypeProjectionImpl(newReturnType)) }
    return replace(newArguments = argumentsWithNewReturnType)
}

fun KotlinType.getValueParameterTypesFromFunctionType(): List<TypeProjection> {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    val arguments = arguments
    val first = contextFunctionTypeParamsCount() + if (isBuiltinExtensionFunctionalType) 1 else 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: $this" }
    return arguments.subList(first, last)
}

fun KotlinType.getValueParameterTypesFromCallableReflectionType(isCallableTypeWithExtension: Boolean): List<TypeProjection> {
    assert(ReflectionTypes.isKCallableType(this)) { "Not a callable reflection type: $this" }
    val arguments = arguments
    val first = if (isCallableTypeWithExtension) 1 else 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: $this" }
    return arguments.subList(first, last)
}

fun KotlinType.extractFunctionalTypeFromSupertypes(): KotlinType {
    assert(isBuiltinFunctionalTypeOrSubtype) { "Not a function type or subtype: $this" }
    return if (isBuiltinFunctionalType) this else supertypes().first { it.isBuiltinFunctionalType }
}

fun KotlinType.getPureArgumentsForFunctionalTypeOrSubtype(): List<KotlinType> {
    assert(isBuiltinFunctionalTypeOrSubtype) { "Not a function type or subtype: $this" }
    return extractFunctionalTypeFromSupertypes().arguments.dropLast(1).map { it.type }
}

fun KotlinType.extractParameterNameFromFunctionTypeArgument(): Name? {
    val annotation = annotations.findAnnotation(StandardNames.FqNames.parameterName) ?: return null
    val name = (annotation.allValueArguments.values.singleOrNull() as? StringValue)
                       ?.value
                       ?.takeIf { Name.isValidIdentifier(it) }
               ?: return null
    return Name.identifier(name)
}

fun getFunctionTypeArgumentProjections(
    receiverType: KotlinType?,
    contextReceiverTypes: List<KotlinType>,
    parameterTypes: List<KotlinType>,
    parameterNames: List<Name>?,
    returnType: KotlinType,
    builtIns: KotlinBuiltIns
): List<TypeProjection> {
    val arguments = ArrayList<TypeProjection>(parameterTypes.size + contextReceiverTypes.size + (if (receiverType != null) 1 else 0) + 1)

    arguments.addAll(contextReceiverTypes.map { it.asTypeProjection() })
    arguments.addIfNotNull(receiverType?.asTypeProjection())

    parameterTypes.mapIndexedTo(arguments) { index, type ->
        val name = parameterNames?.get(index)?.takeUnless { it.isSpecial }
        val typeToUse = if (name != null) {
            val parameterNameAnnotation = BuiltInAnnotationDescriptor(
                builtIns,
                StandardNames.FqNames.parameterName,
                mapOf(Name.identifier("name") to StringValue(name.asString()))
            )
            type.replaceAnnotations(Annotations.create(type.annotations + parameterNameAnnotation))
        }
        else {
            type
        }
        typeToUse.asTypeProjection()
    }

    arguments.add(returnType.asTypeProjection())

    return arguments
}

@JvmOverloads
fun createFunctionType(
    builtIns: KotlinBuiltIns,
    annotations: Annotations,
    receiverType: KotlinType?,
    contextReceiverTypes: List<KotlinType>,
    parameterTypes: List<KotlinType>,
    parameterNames: List<Name>?,
    returnType: KotlinType,
    suspendFunction: Boolean = false
): SimpleType {
    val arguments =
        getFunctionTypeArgumentProjections(receiverType, contextReceiverTypes, parameterTypes, parameterNames, returnType, builtIns)
    val parameterCount = parameterTypes.size + contextReceiverTypes.size + if (receiverType == null) 0 else 1
    val classDescriptor = getFunctionDescriptor(builtIns, parameterCount, suspendFunction)

    // TODO: preserve laziness of given annotations
    var typeAnnotations = annotations
    if (receiverType != null) typeAnnotations = typeAnnotations.withExtensionFunctionAnnotation(builtIns)
    if (contextReceiverTypes.isNotEmpty()) typeAnnotations =
        typeAnnotations.withContextReceiversFunctionAnnotation(builtIns, contextReceiverTypes.size)

    return KotlinTypeFactory.simpleNotNullType(typeAnnotations, classDescriptor, arguments)
}

fun Annotations.hasExtensionFunctionAnnotation() = hasAnnotation(StandardNames.FqNames.extensionFunctionType)

fun Annotations.withoutExtensionFunctionAnnotation() =
    FilteredAnnotations(this, true) { it != StandardNames.FqNames.extensionFunctionType }

fun Annotations.withExtensionFunctionAnnotation(builtIns: KotlinBuiltIns) =
    if (hasAnnotation(StandardNames.FqNames.extensionFunctionType)) {
        this
    } else {
        Annotations.create(this + BuiltInAnnotationDescriptor(builtIns, StandardNames.FqNames.extensionFunctionType, emptyMap()))
    }

fun Annotations.withContextReceiversFunctionAnnotation(builtIns: KotlinBuiltIns, contextReceiversCount: Int) =
    if (hasAnnotation(StandardNames.FqNames.contextFunctionTypeParams)) {
        this
    } else {
        Annotations.create(
            this + BuiltInAnnotationDescriptor(
                builtIns, StandardNames.FqNames.contextFunctionTypeParams, mapOf(
                    Name.identifier("count") to IntValue(contextReceiversCount)
                )
            )
        )
    }

fun getFunctionDescriptor(builtIns: KotlinBuiltIns, parameterCount: Int, isSuspendFunction: Boolean) =
    if (isSuspendFunction) builtIns.getSuspendFunction(parameterCount) else builtIns.getFunction(parameterCount)

fun getKFunctionDescriptor(builtIns: KotlinBuiltIns, parameterCount: Int, isSuspendFunction: Boolean) =
    if (isSuspendFunction) builtIns.getKSuspendFunction(parameterCount) else builtIns.getKFunction(parameterCount)
