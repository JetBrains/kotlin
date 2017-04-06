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

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.descriptors.isUnit
import org.jetbrains.kotlin.backend.konan.isValueType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

// This file describes the ABI for Kotlin descriptors of exported declarations.
// TODO: revise the naming scheme to ensure it produces unique names.
// TODO: do not serialize descriptors of non-exported declarations.

/**
 * Defines whether the declaration is exported, i.e. visible from other modules.
 *
 * Exported declarations must have predictable and stable ABI
 * that doesn't depend on any internal transformations (e.g. IR lowering),
 * and so should be computable from the descriptor itself without checking a backend state.
 */
internal tailrec fun DeclarationDescriptor.isExported(): Boolean {
    if (this.annotations.findAnnotation(symbolNameAnnotation) != null) {
        // Treat any `@SymbolName` declaration as exported.
        return true
    }
    if (this.annotations.findAnnotation(exportForCppRuntimeAnnotation) != null) {
        // Treat any `@ExportForCppRuntime` declaration as exported.
        return true
    }
    if (this.annotations.hasAnnotation(exportForCompilerAnnotation)){
        return true
    }
    if (this.annotations.hasAnnotation(publishedApiAnnotation)){
        return true
    }


    if (this is ConstructorDescriptor && constructedClass.kind.isSingleton) {
        // Currently code generator can access the constructor of the singleton,
        // so ignore visibility of the constructor itself.
        return constructedClass.isExported()
    }

    if (this is DeclarationDescriptorWithVisibility && !this.visibility.isPublicAPI) {
        // If the declaration is explicitly marked as non-public,
        // then it must not be accessible from other modules.
        return false
    }

    if (this is DeclarationDescriptorNonRoot) {
        // If the declaration is not root, then check its container too.
        return containingDeclaration.isExported()
    }

    return true
}

private val symbolNameAnnotation = FqName("konan.SymbolName")

private val exportForCppRuntimeAnnotation = FqName("konan.internal.ExportForCppRuntime")

private val exportForCompilerAnnotation = FqName("konan.internal.ExportForCompiler")

private val publishedApiAnnotation = FqName("kotlin.PublishedApi")

private fun acyclicTypeMangler(visited: MutableSet<TypeParameterDescriptor>, type: KotlinType): String {
    val descriptor = TypeUtils.getTypeParameterDescriptorOrNull(type)
    if (descriptor != null) {
        val upperBounds = if (visited.contains(descriptor)) "" else {

            visited.add(descriptor)

            descriptor.upperBounds.map {
                val bound = acyclicTypeMangler(visited, it)
                if (bound == "kotlin.Any?") "" else "_$bound"
            }.joinToString("")
        }
        return "GENERIC" + upperBounds
    }

    var hashString = TypeUtils.getClassDescriptor(type)!!.fqNameSafe.asString()
    if (!type.arguments.isEmpty()) {
        hashString += "<${type.arguments.map {
            val variance = it.projectionKind.label
            val projection = if (variance == "") "" else "${variance}_" 
            projection + acyclicTypeMangler(visited, it.type)
        }.joinToString(",")}>"
    }

    if (type.isMarkedNullable) hashString += "?"
    return hashString
}

private fun typeToHashString(type: KotlinType) 
    = acyclicTypeMangler(mutableSetOf<TypeParameterDescriptor>(), type)

private val FunctionDescriptor.signature: String
    get() {
        val extensionReceiverPart = this.extensionReceiverParameter?.let { "@${typeToHashString(it.type)}." } ?: ""

        val argsPart = this.valueParameters.map {
            typeToHashString(it.type)
        }.joinToString(";")

        // Just distinguish value types and references - it's needed for calling virtual methods through bridges.
        val returnTypePart =
                when {
                    returnType.let { it != null && it.isValueType() } -> "ValueType"
                    returnType.let { it != null && !KotlinBuiltIns.isUnitOrNullableUnit(it) } -> "Reference"
                    else -> ""
                }

        return "$extensionReceiverPart($argsPart)$returnTypePart"
    }

// TODO: rename to indicate that it has signature included
internal val FunctionDescriptor.functionName: String
    get() = with(this.original) { // basic support for generics
        "$name$signature"
    }

internal val FunctionDescriptor.symbolName: String
    get() {
        if (!this.isExported()) {
            throw AssertionError(this.toString())
        }

        this.annotations.findAnnotation(symbolNameAnnotation)?.let {
            if (this.isExternal) {
                return getStringValue(it)!!
            } else {
                // ignore; TODO: report compile error
            }
        }

        this.annotations.findAnnotation(exportForCppRuntimeAnnotation)?.let {
            val name = getStringValue(it) ?: this.name.asString()
            return name // no wrapping currently required
        }

        val containingDeclarationPart = containingDeclaration.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        return "kfun:$containingDeclarationPart$functionName"
    }

internal val PropertyDescriptor.symbolName: String
    get() {
        val containingDeclarationPart = containingDeclaration.fqNameSafe.let {
            if (it.isRoot) "" else "$it."
        }
        val extensionReceiverPart = this.extensionReceiverParameter?.let { "${it.type}." } ?: ""
        return "kprop:$containingDeclarationPart$extensionReceiverPart$name"

    }

private fun getStringValue(annotation: AnnotationDescriptor): String? {
    annotation.allValueArguments.values.ifNotEmpty {
        val stringValue = this.single() as StringValue
        return stringValue.value
    }

    return null
}

// TODO: bring here dependencies of this method?
internal fun RuntimeAware.getLlvmFunctionType(function: FunctionDescriptor): LLVMTypeRef {
    val original = function.original
    val returnType = if (original is ConstructorDescriptor) voidType else getLLVMReturnType(original.returnType!!)
    val paramTypes = ArrayList(original.allParameters.map { getLLVMType(it.type) })
    if (isObjectType(returnType)) paramTypes.add(kObjHeaderPtrPtr)

    return functionType(returnType, isVarArg = false, paramTypes = *paramTypes.toTypedArray())
}

internal val ClassDescriptor.typeInfoSymbolName: String
    get() {
        assert (this.isExported())
        return "ktype:" + this.fqNameSafe.toString()
    }

internal val theUnitInstanceName = "kobj:kotlin.Unit"

internal val ClassDescriptor.objectInstanceFieldSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjref:$fqNameSafe"
    }
