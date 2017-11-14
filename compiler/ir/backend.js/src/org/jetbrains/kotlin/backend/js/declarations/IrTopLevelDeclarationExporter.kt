/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.declarations

import org.jetbrains.kotlin.backend.js.context.Provider
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.backend.js.util.fqName
import org.jetbrains.kotlin.backend.js.util.name
import org.jetbrains.kotlin.backend.js.util.signatureAsString
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.definePackageAlias
import org.jetbrains.kotlin.name.FqName

class IrTopLevelDeclarationExporter(
        private val names: Provider<IrSymbol, JsName>,
        private val fragment: JsProgramFragment
) : IrElementVisitorVoid {
    private val packageNameMap = mutableMapOf<FqName, JsName>()
    private var currentPackageName: FqName? = null

    private val statements = fragment.exportBlock.statements

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        assert(currentPackageName == null) { "Package fragments can't be nested" }

        currentPackageName = declaration.symbol.fqName
        declaration.acceptChildrenVoid(this)
        currentPackageName = null
    }

    private fun getPackageName(fqName: FqName): JsName = packageNameMap.getOrPut(fqName) {
        if (fqName.isRoot) {
            fragment.scope.declareName("_")
        }
        else {
            JsScope.declareTemporaryName("package$" + fqName.shortName().asString()).also {
                val parentName = getPackageName(fqName.parent())
                statements += definePackageAlias(fqName.shortName().asString(), it, fqName.asString(), parentName.makeRef())
            }
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        val symbol = declaration.symbol
        val name = NameSuggestion.getStableMangledName(symbol.name, symbol.signatureAsString)
        val packageName = getPackageName(currentPackageName ?: FqName.ROOT)
        statements += buildJs { statement(packageName.dot(name).assign(names[symbol].ref())) }
    }

    override fun visitClass(declaration: IrClass) {
        // TODO: export class
        // Don't visit child declarations
    }

    override fun visitExpressionBody(body: IrExpressionBody) {
        // Don't export local declarations
    }
}