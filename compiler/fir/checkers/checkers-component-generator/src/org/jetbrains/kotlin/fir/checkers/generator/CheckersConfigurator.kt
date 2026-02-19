/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.fir.FirElement
import java.io.File
import kotlin.reflect.KClass

class CheckersConfigurator {
    private val registeredAliases: MutableMap<KClass<*>, Pair<String, Boolean>> = LinkedHashMap()
    private val additionalCheckers: MutableMap<String, String> = LinkedHashMap()
    private val visitAlso: MutableMap<KClass<*>, Alias> = LinkedHashMap()

    inline fun <reified T : FirElement> alias(name: String, withVisit: Boolean = true): Alias {
        return alias(T::class, name, withVisit)
    }

    fun alias(kClass: KClass<out FirElement>, name: String, withVisit: Boolean): Alias {
        val realName = name.takeIf { it.startsWith("Fir") } ?: "Fir$name"
        registeredAliases[kClass] = Pair(realName, withVisit)
        return realName
    }

    fun additional(fieldName: String, classFqn: String) {
        additionalCheckers[fieldName] = classFqn
    }

    inline fun <reified T : FirElement> visitAlso(name: String) {
        visitAlso(T::class, name)
    }

    fun visitAlso(kClass: KClass<out FirElement>, by: Alias) {
        visitAlso[kClass] = by
    }

    fun build(): CheckersConfiguration {
        return CheckersConfiguration(registeredAliases, additionalCheckers, visitAlso)
    }
}

fun generateCheckersComponents(
    generationPath: File,
    packageName: String,
    abstractCheckerName: String,
    checkMethodTypeParameterConstraint: KClass<out FirElement>,
    checkType: KClass<out FirElement>,
    init: CheckersConfigurator.() -> Unit,
) {
    val configuration = CheckersConfigurator().apply(init).build()
    Generator(configuration, generationPath, packageName, abstractCheckerName, checkMethodTypeParameterConstraint, checkType).generate()
}
