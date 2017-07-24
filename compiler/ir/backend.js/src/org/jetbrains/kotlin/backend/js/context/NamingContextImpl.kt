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

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.imported
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.naming.SuggestedName
import org.jetbrains.kotlin.js.translate.generateSignature
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isNotNullThrowable

class NamingContextImpl(
        private val currentModule: ModuleDescriptor,
        private val nameSuggestion: NameSuggestion,
        private val rootScope: JsScope,
        private val fragment: JsProgramFragment
) : NamingContext {
    private val tagCache = mutableMapOf<DeclarationDescriptor, String?>()
    private val innerNameCache = mutableMapOf<DeclarationDescriptor, JsName>()
    private val objectInnerNameCache = mutableMapOf<ClassDescriptor, JsName>()
    private val fqnCache = mutableMapOf<DeclarationDescriptor, JsNameRef>()
    private val nameCache = mutableMapOf<DeclarationDescriptor, JsName>()
    private val fieldNameCache = mutableMapOf<PropertyDescriptor, JsName>()
    private val scopeCache = mutableMapOf<ClassDescriptor, JsScope>()

    override val tags = object : Provider<DeclarationDescriptor, String?> {
        override fun get(key: DeclarationDescriptor): String? = tagCache.getOrPut(key) { generateSignature(key) }
    }

    override val innerNames = object : Provider<DeclarationDescriptor, JsName> {
        override fun get(key: DeclarationDescriptor): JsName = innerNameCache.getOrPut(key) {
            if (key is FunctionDescriptor) {
                val initialDescriptor = key.initialSignatureDescriptor
                if (initialDescriptor != null) {
                    return@getOrPut this[initialDescriptor]
                }
            }
            if (key is ModuleDescriptor) {
                return@getOrPut getModuleInnerName(key)
            }
            if (key is LocalVariableDescriptor || key is ParameterDescriptor) {
                return@getOrPut names[key]
            }
            if (key is ConstructorDescriptor) {
                if (key.isPrimary) {
                    return@getOrPut this[key.constructedClass]
                }
            }

            localOrImportedName(key, getSuggestedName(key))
        }
    }

    private fun getScope(descriptor: ClassDescriptor): JsScope = scopeCache.getOrPut(descriptor) {
        if (KotlinBuiltIns.isAny(descriptor)) {
            JsObjectScope(JsProgram().rootScope, "")
        }
        else {
            JsObjectScope(getScope(descriptor.getSuperClassOrAny()), "")
        }
    }

    override val objectInnerNames = object : Provider<ClassDescriptor, JsName> {
        override fun get(key: ClassDescriptor): JsName = objectInnerNameCache.getOrPut(key) {
            val suggested = getSuggestedName(key) + "_getInstance"
            JsScope.declareTemporaryName(suggested).also { result ->
                generateSignature(key)?.let { fragment.nameBindings += JsNameBinding("object:$it", result) }
            }
        }
    }

    override val backingFieldNames = object : Provider<PropertyDescriptor, JsName> {
        override fun get(key: PropertyDescriptor): JsName = fieldNameCache.getOrPut(key) {
            val container = key.containingDeclaration
            if (container is ClassDescriptor) {
                val suggested = nameSuggestion.suggest(key)!!
                val scope = getScope(container)
                val identifier = suggested.names.first()
                scope.declareFreshName("$identifier\$field")
            }
            else {
                val suggested = getSuggestedName(key)
                JsScope.declareTemporaryName(suggested).also { result ->
                    generateSignature(key)?.let { fragment.nameBindings += JsNameBinding("field:$it", result) }
                }
            }
        }
    }

    override val names = object : Provider<DeclarationDescriptor, JsName> {
        override fun get(key: DeclarationDescriptor): JsName {
            val suggested = nameSuggestion.suggest(key) ?:
                            throw IllegalArgumentException("Can't generate name for root declarations: " + key)
            return getActualNameFromSuggested(suggested)[0]
        }
    }

    override val qualifiedReferences = object : Provider<DeclarationDescriptor, JsNameRef> {
        override fun get(key: DeclarationDescriptor): JsNameRef = fqnCache.getOrPut(key) { buildQualifiedExpression(key) }
    }

    private fun localOrImportedName(descriptor: DeclarationDescriptor, suggestedName: String): JsName {
        val module = descriptor.module
        var tag = tags[descriptor]

        val isNative = AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)
        val name = if (module != currentModule || isNative) {
            assert(tag != null) { "Can't import declaration without tag: " + descriptor }
            val result = qualifiedReferences[descriptor]
            val refName = result.name
            if (isNative && result.qualifier == null && refName != null) {
                tag = null
                refName
            }
            else {
                importDeclarationImpl(suggestedName, tag!!, result)
            }
        }
        else {
            JsScope.declareTemporaryName(suggestedName)
        }
        if (tag != null) {
            fragment.nameBindings.add(JsNameBinding(tag, name))
        }
        name.descriptor = descriptor
        return name
    }

    private fun importDeclarationImpl(suggestedName: String, tag: String, declaration: JsExpression): JsName {
        val result = JsScope.declareTemporaryName(suggestedName)
        result.imported = true
        fragment.imports[tag] = declaration
        return result
    }

    private fun getModuleInnerName(descriptor: DeclarationDescriptor): JsName {
        val module = descriptor.module
        if (currentModule == module) {
            return rootScope.declareName("_")
        }
        if (module.builtIns.builtInsModule == module) {
            return rootScope.declareName("Kotlin")
        }
        val nameString = module.name.asString()
        return JsScope.declareTemporaryName(nameString.substring(1, nameString.length - 1))
    }

    private fun buildQualifiedExpression(descriptor: DeclarationDescriptor): JsNameRef {
        if (descriptor is ClassDescriptor) {
            val type = descriptor.defaultType
            if (KotlinBuiltIns.isAny(descriptor)) {
                return buildJs { "Object".refPure() }
            }
            else if (KotlinBuiltIns.isInt(type) || KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isByte(type) ||
                     KotlinBuiltIns.isFloat(type) || KotlinBuiltIns.isDouble(type)) {
                return buildJs { "Number".refPure() }
            }
            else if (KotlinBuiltIns.isLong(type)) {
                return buildJs { "Kotlin".dotPure("Long") }
            }
            else if (KotlinBuiltIns.isChar(type)) {
                return buildJs { "Kotlin".dotPure("BoxedChar") }
            }
            else if (KotlinBuiltIns.isString(type)) {
                return buildJs { "String".refPure() }
            }
            else if (KotlinBuiltIns.isBoolean(type)) {
                return buildJs { "Boolean".refPure() }
            }
            else if (KotlinBuiltIns.isArrayOrPrimitiveArray(descriptor)) {
                return buildJs { "Array".refPure() }
            }
            else if (type.isBuiltinFunctionalType) {
                return buildJs { "Function".refPure() }
            }
            else if (descriptor.defaultType.isNotNullThrowable()) {
                return buildJs { "Error".refPure() }
            }
        }

        val suggested = nameSuggestion.suggest(descriptor)
        if (suggested == null) {
            val module = descriptor.module
            return buildJs { if (module != currentModule) getModuleInnerName(module).refPure() else "_".refPure() }
        }

        var expression: JsNameRef? = if (AnnotationsUtils.isLibraryObject(suggested.descriptor)) {
            buildJs { "Kotlin".refPure() }
        }
        else if (AnnotationsUtils.isNativeObject(suggested.descriptor) && !AnnotationsUtils.isNativeObject(suggested.scope) ||
                 suggested.descriptor is CallableDescriptor && suggested.scope is FunctionDescriptor) {
            null
        }
        else {
            qualifiedReferences[suggested.scope]
        }
        val partNames = getActualNameFromSuggested(suggested)

        for (partName in partNames) {
            expression = JsNameRef(partName, expression)
        }
        return expression!!
    }

    private fun getActualNameFromSuggested(suggested: SuggestedName): List<JsName> {
        var scope = rootScope

        if (suggested.descriptor.isDynamic()) {
            scope = JsDynamicScope
        }
        else if (AnnotationsUtils.isPredefinedObject(suggested.descriptor) && DescriptorUtils.isTopLevelDeclaration(suggested.descriptor)) {
            scope = rootScope
        }

        val names = mutableListOf<JsName>()
        if (suggested.stable) {
            val tag = tags[suggested.descriptor]
            var index = 0
            for (namePart in suggested.names) {
                val name = scope.declareName(namePart)
                name.descriptor = suggested.descriptor
                if (tag != null && !AnnotationsUtils.isNativeObject(suggested.descriptor) &&
                    !AnnotationsUtils.isLibraryObject(suggested.descriptor)) {
                    fragment.nameBindings.add(JsNameBinding(index++.toString() + ":" + tag, name))
                }
                names.add(name)
            }
        }
        else {
            // TODO: consider using sealed class to represent FQNs
            assert(suggested.names.size == 1) { "Private names must always consist of exactly one name" }
            val name = nameCache.getOrPut(suggested.descriptor) {
                var baseName = NameSuggestion.sanitizeName(suggested.names[0])
                if (suggested.descriptor is LocalVariableDescriptor || suggested.descriptor is ValueParameterDescriptor) {
                    JsScope.declareTemporaryName(baseName)
                }
                else {
                    if (!DescriptorUtils.isDescriptorWithLocalVisibility(suggested.descriptor)) {
                        baseName += "_0"
                    }
                    scope.declareFreshName(baseName)
                }
            }
            name.descriptor = suggested.descriptor
            tags[suggested.descriptor]?.let {
                fragment.nameBindings.add(JsNameBinding(it, name))
            }
            names += name
        }

        return names
    }

    private fun getSuggestedName(descriptor: DeclarationDescriptor): String {
        val (suggestedName, container) = when (descriptor) {
            is PropertyGetterDescriptor -> {
                Pair("get_${descriptor.correspondingProperty.name}", descriptor.correspondingProperty.containingDeclaration)
            }
            is PropertySetterDescriptor -> {
                Pair("set_${descriptor.correspondingProperty.name}", descriptor.correspondingProperty.containingDeclaration)
            }
            is ConstructorDescriptor -> {
                return getSuggestedName(descriptor.containingDeclaration) + "_init"
            }
            else -> {
                val name = if (descriptor.name.isSpecial) {
                    when (descriptor) {
                        is ClassDescriptor -> if (DescriptorUtils.isAnonymousObject(descriptor)) "ObjectLiteral" else "Anonymous"
                        is FunctionDescriptor -> "lambda"
                        else -> "anonymous"
                    }
                }
                else {
                    NameSuggestion.sanitizeName(descriptor.name.asString())
                }
                Pair(name, descriptor.containingDeclaration)
            }
        }

        return if (container != null && descriptor !is PackageFragmentDescriptor && !DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            getSuggestedName(container) + "$" + NameSuggestion.sanitizeName(suggestedName)
        }
        else {
            NameSuggestion.sanitizeName(suggestedName)
        }
    }
}