/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.generators.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable

interface Fir2IrComponents {
    val session: FirSession
    val scopeSession: ScopeSession

    /**
     * It's important to use this fir provider in fir2ir instead of provider from session,
     *   because this provider will also contain synthetic fir files for declarations generated
     *   by frontend plugins
     */
    val firProvider: FirProviderWithGeneratedFiles

    val converter: Fir2IrConverter

    val builtins: Fir2IrBuiltinSymbolsContainer
    val specialAnnotationsProvider: IrSpecialAnnotationsProvider?
    val manglers: Manglers

    val irFactory: IrFactory
    val irProviders: List<IrProvider>
    val lock: IrLock

    val classifierStorage: Fir2IrClassifierStorage
    val declarationStorage: Fir2IrDeclarationStorage

    val typeConverter: Fir2IrTypeConverter
    val visibilityConverter: Fir2IrVisibilityConverter

    val callablesGenerator: Fir2IrCallableDeclarationsGenerator
    val classifiersGenerator: Fir2IrClassifiersGenerator
    val lazyDeclarationsGenerator: Fir2IrLazyDeclarationsGenerator
    val dataClassMembersGenerator: Fir2IrDataClassMembersGenerator

    val annotationGenerator: AnnotationGenerator
    val callGenerator: CallAndReferenceGenerator
    val lazyFakeOverrideGenerator: Fir2IrLazyFakeOverrideGenerator
    val symbolsMappingForLazyClasses: Fir2IrSymbolsMappingForLazyClasses

    val extensions: Fir2IrExtensions
    val configuration: Fir2IrConfiguration

    val annotationsFromPluginRegistrar: Fir2IrIrGeneratedDeclarationsRegistrar

    /**
     * A set of FIR files serving as input for the fir2ir ([Fir2IrConverter.generateIrModuleFragment] function) for conversion to IR.
     *
     * We set annotations for IR objects, such as IrFunction, in two scenarios:
     *  1. For FIR declared in library or precompiled: when creating IR object from FIR
     *  2. For FIR declared in a source module: when filling contents of IR object in Fir2IrVisitor
     *
     * Since Fir2IrVisitor will recursively visit all FIR objects and generate IR objects for them, we handle the first scenario
     * above as a corner case.
     *
     * However, when we use CodeGen analysis API, even FIRs declared in the source module can be out of the compile target files,
     * because we can run the CodeGen only for a few files of the source module. We use [filesBeingCompiled] for that case
     * to determine whether a given FIR is declared in a source file to be compiled or not for the CodeGen API. If it is not
     * declared in a file to be compiled (i.e., target of CodeGen), we have to set annotations for IR when creating its IR like
     * the first scenario above. We set [filesBeingCompiled] as `null` if we do not use the CodeGen analysis API.
     */
    val filesBeingCompiled: Set<FirFile>?

    interface Manglers {
        val irMangler: KotlinMangler.IrMangler
        val firMangler: FirMangler
    }
}
