/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.FqName


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
abstract class FirProvidedDeclarationsForMetadataService : FirSessionComponent {
    companion object {
        fun create(session: FirSession): FirProvidedDeclarationsForMetadataService {
            return FirProvidedDeclarationsForMetadataServiceImpl(session)
        }
    }

    abstract fun getProvidedTopLevelDeclarations(packageFqName: FqName): List<FirDeclaration>
    abstract fun getProvidedTopLevelDeclarations(file: FirFile): List<FirDeclaration>
    abstract fun getProvidedConstructors(owner: FirClassSymbol<*>): List<FirConstructor>
    abstract fun getProvidedCallables(owner: FirClassSymbol<*>): List<FirCallableDeclaration>
    abstract fun getProvidedNestedClasses(owner: FirClassSymbol<*>): List<FirRegularClass>
    abstract fun getProvidedCompanionObject(owner: FirClassSymbol<*>): FirRegularClass?

    abstract fun registerDeclaration(declaration: FirDeclaration, containingDeclaration: FirDeclaration)
}

private class FirProvidedDeclarationsForMetadataServiceImpl(private val session: FirSession) : FirProvidedDeclarationsForMetadataService() {
    private val topLevelsCacheByPackage: MutableMap<FqName, MutableList<FirDeclaration>> = hashMapOf()
    private val topLevelsCacheByFile: MutableMap<FirFile, MutableList<FirDeclaration>> = hashMapOf()

    private val memberCache: MutableMap<FirClassSymbol<*>, ClassDeclarations> = hashMapOf()

    override fun registerDeclaration(declaration: FirDeclaration, containingDeclaration: FirDeclaration) {
        when (containingDeclaration) {
            is FirFile -> {
                topLevelsCacheByFile.getOrPut(containingDeclaration) { mutableListOf() }.add(declaration)
                topLevelsCacheByPackage.getOrPut(containingDeclaration.packageFqName) { mutableListOf() }.add(declaration)
            }

            is FirRegularClass -> {
                val declarations = memberCache.getOrPut(containingDeclaration.symbol) { ClassDeclarations() }
                when (declaration) {
                    is FirConstructor -> declarations.providedConstructors += declaration
                    is FirCallableDeclaration -> declarations.providedCallables += declaration
                    is FirRegularClass -> {
                        if (declaration.isCompanion) {
                            require(declarations.providedCompanionObject == null) {
                                "Class ${containingDeclaration.symbol.classId} already has a provided companion object; cannot register another"
                            }
                            declarations.providedCompanionObject = declaration
                        } else {
                            declarations.providedNestedClasses += declaration
                        }
                    }
                    else -> error("Unsupported declaration type: ${declaration::class.simpleName}")
                }
            }

            else -> error("Containing declaration must be either `FirFile` or `FirRegularClass`, but got ${containingDeclaration.render()}")
        }
    }

    override fun getProvidedTopLevelDeclarations(packageFqName: FqName): List<FirDeclaration> {
        return topLevelsCacheByPackage[packageFqName] ?: emptyList()
    }

    override fun getProvidedTopLevelDeclarations(file: FirFile): List<FirDeclaration> {
        return topLevelsCacheByFile[file] ?: emptyList()
    }

    override fun getProvidedConstructors(owner: FirClassSymbol<*>): List<FirConstructor> {
        return memberCache[owner]?.providedConstructors ?: emptyList()
    }

    override fun getProvidedCallables(owner: FirClassSymbol<*>): List<FirCallableDeclaration> {
        return memberCache[owner]?.providedCallables ?: emptyList()
    }

    override fun getProvidedNestedClasses(owner: FirClassSymbol<*>): List<FirRegularClass> {
        return memberCache[owner]?.providedNestedClasses ?: emptyList()
    }

    override fun getProvidedCompanionObject(owner: FirClassSymbol<*>): FirRegularClass? {
        return memberCache[owner]?.providedCompanionObject
    }

    private class ClassDeclarations {
        val providedCallables: MutableList<FirCallableDeclaration> = mutableListOf()
        val providedConstructors: MutableList<FirConstructor> = mutableListOf()
        val providedNestedClasses: MutableList<FirRegularClass> = mutableListOf()
        var providedCompanionObject: FirRegularClass? = null
    }
}

val FirSession.providedDeclarationsForMetadataService: FirProvidedDeclarationsForMetadataService by FirSession.sessionComponentAccessor()
