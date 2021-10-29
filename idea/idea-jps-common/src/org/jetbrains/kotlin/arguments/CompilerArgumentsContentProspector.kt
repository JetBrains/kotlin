// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.arguments

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.InternalArgument
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

object CompilerArgumentsContentProspector {
    private val argumentPropertiesCache: MutableMap<KClass<out CommonToolArguments>, List<KProperty1<out CommonToolArguments, *>>> =
        mutableMapOf()

    private val flagArgumentPropertiesCache: MutableMap<KClass<out CommonToolArguments>, List<KProperty1<out CommonToolArguments, Boolean>>> =
        mutableMapOf()

    private val stringArgumentPropertiesCache: MutableMap<KClass<out CommonToolArguments>, List<KProperty1<out CommonToolArguments, String?>>> =
        mutableMapOf()

    private val arrayArgumentPropertiesCache: MutableMap<KClass<out CommonToolArguments>, List<KProperty1<out CommonToolArguments, Array<String>?>>> =
        mutableMapOf()

    private fun getCompilerArguments(kClass: KClass<out CommonToolArguments>) = argumentPropertiesCache.getOrPut(kClass) {
        kClass.memberProperties.filter { prop -> prop.annotations.any { it is Argument } }
    }

    private inline fun <reified R : Any?> List<KProperty1<out CommonToolArguments, *>>.filterByReturnType(predicate: (KType?) -> Boolean) =
        filter { predicate(it.returnType) }.mapNotNull { it.safeAs<KProperty1<CommonToolArguments, R>>() }

    fun getFlagCompilerArgumentProperties(kClass: KClass<out CommonToolArguments>): List<KProperty1<out CommonToolArguments, Boolean>> =
        flagArgumentPropertiesCache.getOrPut(kClass) { getCompilerArguments(kClass).filterByReturnType { it?.classifier == Boolean::class } }

    fun <T : CommonToolArguments> getStringCompilerArgumentProperties(kClass: KClass<T>): List<KProperty1<out CommonToolArguments, String?>> =
        stringArgumentPropertiesCache.getOrPut(kClass) { getCompilerArguments(kClass).filterByReturnType { it?.classifier == String::class } }

    fun <T : CommonToolArguments> getArrayCompilerArgumentProperties(kClass: KClass<T>): List<KProperty1<out CommonToolArguments, Array<String>?>> =
        arrayArgumentPropertiesCache.getOrPut(kClass) { getCompilerArguments(kClass).filterByReturnType { (it?.classifier as? KClass<*>)?.java?.isArray == true } }

    val freeArgsProperty: KProperty1<in CommonToolArguments, List<String>>
        get() = CommonToolArguments::freeArgs
    val internalArgumentsProperty: KProperty1<in CommonToolArguments, List<InternalArgument>>
        get() = CommonToolArguments::internalArguments
}