/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import kotlin.reflect.KClass

/*
 * TODO:
 *  - add registration of new files
 *  - check that annotations or meta-annotations is not empty
 */
abstract class FirClassGenerationExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("StatusTransformer")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val mode: Mode = Mode.ANNOTATED_ELEMENT

    final override val extensionType: KClass<out FirExtension> = FirClassGenerationExtension::class

    abstract fun <T> generateClass(
        containingFile: FirFile,
        annotatedDeclaration: T
    ): List<GeneratedClass> where T : FirDeclaration, T : FirAnnotationContainer

    data class GeneratedClass(val klass: FirRegularClass, val container: FirDeclaration) {
        init {
            require(container is FirRegularClass || container is FirFile)
        }
    }

    fun interface Factory : FirExtension.Factory<FirClassGenerationExtension>
}

val FirExtensionsService.classGenerationExtensions: FirExtensionsService.ExtensionsAccessor<FirClassGenerationExtension> by FirExtensionsService.registeredExtensions()
