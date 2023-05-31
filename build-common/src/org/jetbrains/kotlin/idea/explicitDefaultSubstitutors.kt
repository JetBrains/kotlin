/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

//used by IJ facet import
@SuppressWarnings("unused")
val defaultSubstitutors: Map<KClass<out CommonToolArguments>, Collection<ExplicitDefaultSubstitutor>> = emptyMap()

sealed class ExplicitDefaultSubstitutor {
    abstract val substitutedProperty: KProperty1<out CommonToolArguments, String?>
    abstract val oldSubstitution: List<String>
    abstract val newSubstitution: List<String>
    abstract fun isSubstitutable(args: List<String>): Boolean

    protected val argument: Argument by lazy {
        substitutedProperty.javaField?.getAnnotation(Argument::class.java)
            ?: error("Property \"${substitutedProperty.name}\" has no Argument annotation")
    }
}

@Deprecated(message = "Minimal supported jvmTarget version is 1.8")
object JvmTargetDefaultSubstitutor : ExplicitDefaultSubstitutor() {
    override val substitutedProperty
        get() = K2JVMCompilerArguments::jvmTarget
    private val oldDefault: String
        get() = JvmTarget.JVM_1_6.description
    private val newDefault: String
        get() = JvmTarget.JVM_1_8.description

    private fun prepareSubstitution(default: String): List<String> = listOf(argument.value, default)

    override val oldSubstitution: List<String>
        get() = prepareSubstitution(oldDefault)
    override val newSubstitution: List<String>
        get() = prepareSubstitution(newDefault)

    override fun isSubstitutable(args: List<String>): Boolean = argument.value !in args
}
