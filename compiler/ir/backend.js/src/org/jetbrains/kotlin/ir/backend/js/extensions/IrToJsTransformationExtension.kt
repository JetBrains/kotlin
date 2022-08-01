/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrModule
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsModuleOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.JsImportedModule
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.serialization.js.ModuleKind

/**
 * 1. Ability to generate additional JsIrModule ([???])
 * 2. Ability to add additional roots to DCE (in [org.jetbrains.kotlin.ir.backend.js.dce.DceKt.buildRoots])
 * 3. Intercept module wrapping (in [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.ModuleWrapperTranslation.wrap])
 *
 * Also, we need some abstraction for plugins in dependency resolution (which is hard :c) [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CrossModuleDependenciesResolver.resolveCrossModuleDependencies]
 */
interface IrToJsTransformationExtension {
    companion object : ProjectExtensionDescriptor<IrToJsTransformationExtension>(
        "org.jetbrains.kotlin.jsGenerationExtension", IrToJsTransformationExtension::class.java
    )

    fun generateAdditionalJsIrModules(
        module: IrModuleFragment,
        context: JsIrBackendContext,
        minimizedMemberNames: Boolean
    ): List<JsIrModule> = emptyList()

    fun getAdditionalDceRoots(module: IrModuleFragment): List<IrDeclaration> = emptyList()

    fun transformMainFunction(mainInvocation: JsStatement): JsStatement = mainInvocation

    fun postprocessJsAst(
        jsProgram: JsProgram,
        moduleKind: ModuleKind,
        moduleOrigin: JsModuleOrigin,
        importedJsModules: List<JsImportedModule>
    ) {}

    val extensionKey: IrToJsExtensionKey
}