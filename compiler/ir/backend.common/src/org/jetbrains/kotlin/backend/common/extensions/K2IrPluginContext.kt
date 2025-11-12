/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs

interface K2IrPluginContext : IrGeneratorContext {
    /**
     * [irFactory] is a service used for creating new IR declarations.
     */
    override val irFactory: IrFactory

    /**
     * [irBuiltIns] provides access to some builtin types and declarations like [kotlin.Int]
     */
    override val irBuiltIns: IrBuiltIns

    /**
     * [languageVersionSettings] contains information about the language version,
     * specific language features and analysis flags configured for the current compilation.
     */
    val languageVersionSettings: LanguageVersionSettings

    /**
     * [platform] contains information about the target platform.
     * Utilities like [TargetPlatform.isJs] could be used to check for specific target.
     */
    val platform: TargetPlatform

    /**
     * Returns a diagnostic reporter instance to report IR diagnostics from plugin
     */
    val diagnosticReporter: IrDiagnosticReporter

    /**
     * Use this service to:
     * - add annotations to declarations if those annotations should be saved into metadata
     * - register that declaration generated at IR stage will appear in compiled metadata
     * This service properly works only in K2 compiler
     */
    val metadataDeclarationRegistrar: IrGeneratedDeclarationsRegistrar

    /**
     * Returns a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns its expansion.
     *
     * If you need to access a not-expanded typealias, use [referenceClassifier] instead.
     *
     * @param fromFile the file from which the reference is made. This information is needed
     * for proper reference collecting for incremental compilation.
     */
    fun referenceClass(classId: ClassId, fromFile: IrFile): IrClassSymbol?

    /**
     * Returns a class or typealias associated with given [classId].
     *
     * @param fromFile the file from which the reference is made. This information is needed
     * for proper reference collecting for incremental compilation.
     */
    fun referenceClassifier(classId: ClassId, fromFile: IrFile): IrSymbol?

    /**
     * Returns constructors of a class associated with given [classId].
     * If there is a typealias with the [classId], this function returns constructors of its expansion.
     *
     * @param fromFile the file from which the reference is made. This information is needed
     * for proper reference collecting for incremental compilation.
     */
    fun referenceConstructors(classId: ClassId, fromFile: IrFile): Collection<IrConstructorSymbol>

    /**
     * Returns functions with given [callableId].
     *
     * @param fromFile the file from which the reference is made. This information is needed
     * for proper reference collecting for incremental compilation.
     */
    fun referenceFunctions(callableId: CallableId, fromFile: IrFile): Collection<IrSimpleFunctionSymbol>

    /**
     * Returns properties with given [callableId].
     *
     * @param fromFile the file from which the reference is made. This information is needed
     * for proper reference collecting for incremental compilation.
     */
    fun referenceProperties(callableId: CallableId, fromFile: IrFile): Collection<IrPropertySymbol>

    /**
     * Records information that [declaration] was referenced during modification of file [fromFile].
     * This information later will be used by the Incremental compilation to correctly invalidate
     * compiled files on source changes.
     *
     * Lookup recording is incorporated into `referenceXxx` functions, so usually there is no need to call
     * this function directly.
     */
    @InternalApi
    fun recordLookup(declaration: IrDeclarationWithName, fromFile: IrFile)

    @RequiresOptIn
    annotation class InternalApi
}
