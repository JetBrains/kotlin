/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.name.FqName
import kotlin.reflect.KClass

/**
 * This extension can be used to provide information about declarations, which should be written in Kotlin metadata (i.e. be part of a module's ABI),
 *   but for some reason can not (or should not) be generated with [FirDeclarationGenerationExtension]
 *
 * Example: assume your plugin generates some constructor in [IrGenerationExtension] which have value parameters matching
 *   all properties of a class, and this constructor is used only as an implementation detail (so the only actor who accesses it
 *   is a plugin itself). The constructor should be accessible from another module, so it should be present in the metadata. But you
 *   can not generate this constructor in [FirDeclarationGenerationExtension], because it depends on types of properties,
 *   which may be not resolved at the moment of constructor creation.
 *
 *
 *   // MODULE: a
 *   open class Base {
 *       val x: Int = 1
 *       val y = "hello"
 *
 *       constructor()
 *
 *       // generated
 *       constructor(x: Int, y: String) { ... } // (1)
 *   }
 *
 *   // MODULE: b(a)
 *   class Derived : Base {
 *       val z = 1.0
 *
 *       constructor() : super()
 *
 *       // generated
 *       constructor(x: Int, y: String, z: Double) : super(x, y) { ... } // (2)
 *
 *       // constructor (1) should be presented in metadata of class Base, so IR plugin
 *       //  can reference it during generation of constructor (2)
 *  }
 *
 * All declarations provided by this extension should be fully resolved and contain all sub declarations if they exist.
 *   E.g. if you want to provide some class then this class should contain all declarations of this class you want to be
 *   present in metadata in `declarations` field of a `FirRegularClass`.
 *   [FirDeclarationGenerationExtension] won't be called for classes provided by this extension.
 */
abstract class FirDeclarationsForMetadataProviderExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("SyntheticDeclarationsForMetadataGenerationExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirDeclarationsForMetadataProviderExtension::class

    /**
     * It's allowed to provide only functions, properties and type aliases
     */
    open fun provideTopLevelDeclarations(packageFqName: FqName, scopeSession: ScopeSession): List<FirDeclaration> = emptyList()

    open fun provideDeclarationsForClass(klass: FirClass, scopeSession: ScopeSession): List<FirDeclaration> = emptyList()

    fun interface Factory : FirExtension.Factory<FirDeclarationsForMetadataProviderExtension>
}

val FirExtensionService.declarationForMetadataProviders: List<FirDeclarationsForMetadataProviderExtension> by FirExtensionService.registeredExtensions()
