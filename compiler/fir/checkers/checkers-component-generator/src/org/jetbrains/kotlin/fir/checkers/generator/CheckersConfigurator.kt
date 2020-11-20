/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.FirElement
import java.io.File
import kotlin.reflect.KClass

class CheckersConfigurator {
    private val registeredAliases: MutableMap<KClass<*>, String> = LinkedHashMap()
    private val additionalCheckers: MutableMap<String, String> = LinkedHashMap()

    inline fun <reified T : FirElement> alias(name: String) {
        alias(T::class, name)
    }

    fun alias(kClass: KClass<out FirElement>, name: String) {
        val realName = name.takeIf { it.startsWith("Fir") } ?: "Fir$name"
        registeredAliases[kClass] = realName
    }

    fun additional(fieldName: String, classFqn: String) {
        additionalCheckers[fieldName] = classFqn
    }

    fun build(): CheckersConfiguration {
        return CheckersConfiguration(registeredAliases, additionalCheckers)
    }
}

fun generateCheckersComponents(
    generationPath: File,
    packageName: String,
    abstractCheckerName: String,
    init: CheckersConfigurator.() -> Unit
) {
    val configuration = CheckersConfigurator().apply(init).build()
    Generator(configuration, generationPath, packageName, abstractCheckerName).generate()
}
