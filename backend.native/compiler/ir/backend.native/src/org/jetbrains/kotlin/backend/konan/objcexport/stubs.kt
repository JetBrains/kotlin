/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

open class Stub<out D : DeclarationDescriptor>(val name: String, val descriptor: D?)

abstract class ObjCClass<out D : DeclarationDescriptor>(name: String,
                                                        descriptor: D?,
                                                        val superProtocols: List<String>,
                                                        val members: List<Stub<*>>,
                                                        val attributes: List<String>) : Stub<D>(name, descriptor)

class ObjCProtocol(name: String,
                   descriptor: ClassDescriptor,
                   superProtocols: List<String>,
                   members: List<Stub<*>>,
                   attributes: List<String> = emptyList()) : ObjCClass<ClassDescriptor>(name, descriptor, superProtocols, members, attributes)

class ObjCInterface(name: String,
                    val generics: List<String> = emptyList(),
                    descriptor: ClassDescriptor? = null,
                    val superClass: String? = null,
                    val superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
                    superProtocols: List<String> = emptyList(),
                    val categoryName: String? = null,
                    members: List<Stub<*>> = emptyList(),
                    attributes: List<String> = emptyList()) : ObjCClass<ClassDescriptor>(name, descriptor, superProtocols, members, attributes)

class ObjCMethod(descriptor: DeclarationDescriptor?,
                 val isInstanceMethod: Boolean,
                 val returnType: ObjCType,
                 val selectors: List<String>,
                 val parameters: List<ObjCParameter>,
                 val attributes: List<String>) : Stub<DeclarationDescriptor>(buildMethodName(selectors, parameters), descriptor)

class ObjCParameter(name: String,
                    descriptor: ParameterDescriptor?,
                    val type: ObjCType) : Stub<ParameterDescriptor>(name, descriptor)

class ObjCProperty(name: String,
                   descriptor: PropertyDescriptor?,
                   val type: ObjCType,
                   val propertyAttributes: List<String>,
                   val setterName: String? = null,
                   val getterName: String? = null,
                   val declarationAttributes: List<String> = emptyList()) : Stub<PropertyDescriptor>(name, descriptor) {

    @Deprecated("", ReplaceWith("this.propertyAttributes"), DeprecationLevel.WARNING)
    val attributes: List<String> get() = propertyAttributes
}

private fun buildMethodName(selectors: List<String>, parameters: List<ObjCParameter>): String =
        if (selectors.size == 1 && parameters.size == 0) {
            selectors[0]
        } else {
            assert(selectors.size == parameters.size)
            selectors.joinToString(separator = "")
        }
