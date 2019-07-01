/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.RESERVED_IDENTIFIERS
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.naming.isValidES5Identifier
import org.jetbrains.kotlin.serialization.js.ModuleKind

object ModuleWrapperTranslation {
    object Namer {
        fun requiresEscaping(name: String) =
            !name.isValidES5Identifier() || name in RESERVED_IDENTIFIERS
    }

    fun wrap(
        moduleId: String, function: JsExpression, importedModules: List<JsImportedModule>,
        program: JsProgram, kind: ModuleKind
    ): List<JsStatement> {
        return when (kind) {
            ModuleKind.AMD -> wrapAmd(function, importedModules, program)
            ModuleKind.COMMON_JS -> wrapCommonJs(function, importedModules, program)
            ModuleKind.UMD -> wrapUmd(moduleId, function, importedModules, program)
            ModuleKind.PLAIN -> wrapPlain(moduleId, function, importedModules, program)
        }
    }

    private fun wrapUmd(
        moduleId: String, function: JsExpression,
        importedModules: List<JsImportedModule>, program: JsProgram
    ): List<JsStatement> {
        val scope = program.scope
        val defineName = scope.declareName("define")
        val exportsName = scope.declareName("exports")

        val adapterBody = JsBlock()
        val adapter = JsFunction(program.scope, adapterBody, "Adapter")
        val rootName = adapter.scope.declareName("root")
        val factoryName = adapter.scope.declareName("factory")
        adapter.parameters += JsParameter(rootName)
        adapter.parameters += JsParameter(factoryName)

        val amdTest = JsAstUtils.and(JsAstUtils.typeOfIs(defineName.makeRef(), JsStringLiteral("function")),
                                     JsNameRef("amd", defineName.makeRef()))
        val commonJsTest = JsAstUtils.typeOfIs(exportsName.makeRef(), JsStringLiteral("object"))

        val amdBody = JsBlock(wrapAmd(factoryName.makeRef(), importedModules, program))
        val commonJsBody = JsBlock(wrapCommonJs(factoryName.makeRef(), importedModules, program))
        val plainInvocation = makePlainInvocation(moduleId, factoryName.makeRef(), importedModules, program)

        val lhs: JsExpression = if (Namer.requiresEscaping(moduleId)) {
            JsArrayAccess(rootName.makeRef(), JsStringLiteral(moduleId))
        }
        else {
            JsNameRef(scope.declareName(moduleId), rootName.makeRef())
        }

        val plainBlock = JsBlock()
        for (importedModule in importedModules) {
            plainBlock.statements += addModuleValidation(moduleId, program, importedModule)
        }
        plainBlock.statements += JsAstUtils.assignment(lhs, plainInvocation).makeStmt()

        val selector = JsAstUtils.newJsIf(amdTest, amdBody, JsAstUtils.newJsIf(commonJsTest, commonJsBody, plainBlock))
        adapterBody.statements += selector

        return listOf(JsInvocation(adapter, JsThisRef(), function).makeStmt())
    }

    private fun wrapAmd(
        function: JsExpression,
        importedModules: List<JsImportedModule>, program: JsProgram
    ): List<JsStatement> {
        val scope = program.scope
        val defineName = scope.declareName("define")
        val invocationArgs = listOf(
            JsArrayLiteral(listOf(JsStringLiteral("exports")) + importedModules.map { JsStringLiteral(it.externalName) }),
            function
        )

        val invocation = JsInvocation(defineName.makeRef(), invocationArgs)
        return listOf(invocation.makeStmt())
    }

    private fun wrapCommonJs(
        function: JsExpression,
        importedModules: List<JsImportedModule>,
        program: JsProgram
    ): List<JsStatement> {
        val scope = program.scope
        val moduleName = scope.declareName("module")
        val requireName = scope.declareName("require")

        val invocationArgs = importedModules.map { JsInvocation(requireName.makeRef(), JsStringLiteral(it.externalName)) }
        val invocation = JsInvocation(function, listOf(JsNameRef("exports", moduleName.makeRef())) + invocationArgs)
        return listOf(invocation.makeStmt())
    }

    private fun wrapPlain(
        moduleId: String, function: JsExpression,
        importedModules: List<JsImportedModule>, program: JsProgram
    ): List<JsStatement> {
        val invocation = makePlainInvocation(moduleId, function, importedModules, program)
        val statements = mutableListOf<JsStatement>()

        for (importedModule in importedModules) {
            statements += addModuleValidation(moduleId, program, importedModule)
        }

        statements += if (Namer.requiresEscaping(moduleId)) {
            JsAstUtils.assignment(makePlainModuleRef(moduleId, program), invocation).makeStmt()
        }
        else {
            JsAstUtils.newVar(program.rootScope.declareName(moduleId), invocation)
        }

        return statements
    }

    private fun addModuleValidation(
        currentModuleId: String,
        program: JsProgram,
        module: JsImportedModule
    ): JsStatement {
        val moduleRef = makePlainModuleRef(module, program)
        val moduleExistsCond = JsAstUtils.typeOfIs(moduleRef, JsStringLiteral("undefined"))
        val moduleNotFoundMessage = JsStringLiteral(
            "Error loading module '" + currentModuleId + "'. Its dependency '" + module.externalName + "' was not found. " +
                    "Please, check whether '" + module.externalName + "' is loaded prior to '" + currentModuleId + "'.")
        val moduleNotFoundThrow = JsThrow(JsNew(JsNameRef("Error"), listOf<JsExpression>(moduleNotFoundMessage)))
        return JsIf(moduleExistsCond, JsBlock(moduleNotFoundThrow))
    }

    private fun makePlainInvocation(
        moduleId: String,
        function: JsExpression,
        importedModules: List<JsImportedModule>,
        program: JsProgram
    ): JsInvocation {
        val invocationArgs = importedModules.map { makePlainModuleRef(it, program) }
        val moduleRef = makePlainModuleRef(moduleId, program)
        val testModuleDefined = JsAstUtils.typeOfIs(moduleRef, JsStringLiteral("undefined"))
        val selfArg = JsConditional(testModuleDefined, JsObjectLiteral(false), moduleRef.deepCopy())

        return JsInvocation(function, listOf(selfArg) + invocationArgs)
    }

    private fun makePlainModuleRef(module: JsImportedModule, program: JsProgram): JsExpression {
        return module.plainReference ?: makePlainModuleRef(module.externalName, program)
    }

    private fun makePlainModuleRef(moduleId: String, program: JsProgram): JsExpression {
        // TODO: we could use `this.moduleName` syntax. However, this does not work for `kotlin` module in Rhino, since
        // we run kotlin.js in a parent scope. Consider better solution
        return if (Namer.requiresEscaping(moduleId)) {
            JsArrayAccess(JsThisRef(), JsStringLiteral(moduleId))
        }
        else {
            program.scope.declareName(moduleId).makeRef()
        }
    }
}
