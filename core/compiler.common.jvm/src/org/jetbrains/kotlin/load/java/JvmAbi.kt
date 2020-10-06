/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

object JvmAbi {
    const val DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls"
    const val ERASED_INLINE_CONSTRUCTOR_NAME = "constructor"

    @JvmField
    val JVM_FIELD_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmField")

    /**
     * Warning: use DEFAULT_IMPLS_CLASS_NAME and TypeMappingConfiguration.innerClassNameFactory when possible.
     * This is false for KAPT3 mode.
     */
    const val DEFAULT_IMPLS_SUFFIX = "$$DEFAULT_IMPLS_CLASS_NAME"

    const val DEFAULT_PARAMS_IMPL_SUFFIX = "\$default"

    private const val GET_PREFIX = "get"
    private const val IS_PREFIX = "is"
    private const val SET_PREFIX = "set"

    const val DELEGATED_PROPERTY_NAME_SUFFIX = "\$delegate"
    const val DELEGATED_PROPERTIES_ARRAY_NAME = "$\$delegatedProperties"
    const val DELEGATE_SUPER_FIELD_PREFIX = "$\$delegate_"
    private const val ANNOTATIONS_SUFFIX = "\$annotations"
    const val ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX = ANNOTATIONS_SUFFIX
    private const val ANNOTATED_TYPEALIAS_METHOD_NAME_SUFFIX = ANNOTATIONS_SUFFIX

    const val INSTANCE_FIELD = "INSTANCE"
    const val HIDDEN_INSTANCE_FIELD = "$$$INSTANCE_FIELD"

    val REFLECTION_FACTORY_IMPL = ClassId.topLevel(FqName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl"))

    const val LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT = "\$i\$a$"
    const val LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION = "\$i\$f$"

    const val IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS = "-impl"

    /**
     * @param baseName JVM name of the property getter since Kotlin 1.4, or Kotlin name of the property otherwise.
     */
    @JvmStatic
    fun getSyntheticMethodNameForAnnotatedProperty(baseName: String): String {
        return baseName + ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX
    }

    @JvmStatic
    fun getSyntheticMethodNameForAnnotatedTypeAlias(typeAliasName: Name): String {
        return typeAliasName.asString() + ANNOTATED_TYPEALIAS_METHOD_NAME_SUFFIX
    }

    @JvmStatic
    fun isGetterName(name: String): Boolean {
        return name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)
    }

    @JvmStatic
    fun isSetterName(name: String): Boolean {
        return name.startsWith(SET_PREFIX)
    }

    @JvmStatic
    fun getterName(propertyName: String): String {
        return if (startsWithIsPrefix(propertyName)) propertyName else GET_PREFIX + propertyName.capitalizeAsciiOnly()
    }

    @JvmStatic
    fun setterName(propertyName: String): String {
        return SET_PREFIX +
                if (startsWithIsPrefix(propertyName)) propertyName.substring(IS_PREFIX.length) else propertyName.capitalizeAsciiOnly()
    }

    @JvmStatic
    fun startsWithIsPrefix(name: String): Boolean {
        if (!name.startsWith(IS_PREFIX)) return false
        if (name.length == IS_PREFIX.length) return false
        val c = name[IS_PREFIX.length]
        return !('a' <= c && c <= 'z')
    }
}
