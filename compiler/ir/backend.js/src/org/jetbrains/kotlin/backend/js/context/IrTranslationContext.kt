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

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class IrTranslationContext(val config: IrTranslationConfig, val fragment: JsProgramFragment) {
    var statements: MutableCollection<JsStatement> = mutableListOf()
    private var mutableScope = config.scope
    val nameSuggestion = NameSuggestion()
    private val aliasesImpl = mutableMapOf<DeclarationDescriptor, JsExpression>()
    val naming: NamingContext = NamingContextImpl(config.module.descriptor, nameSuggestion, config.scope, fragment)
    var loops = mutableMapOf<IrLoop, LoopState>()

    val scope: JsScope get() = mutableScope
    var currentDeclaration: MemberDescriptor? = null
    var currentClass: ClassDescriptor? = null
    val module get() = config.module

    val exporter = DeclarationExporter(naming, nameSuggestion, fragment, config.jsConfig)

    val aliases = object : Provider<DeclarationDescriptor, JsExpression?> {
        override fun get(key: DeclarationDescriptor): JsExpression? = aliasesImpl[key]
    }

    fun addStatements(statements: Collection<JsStatement>) {
        this.statements.addAll(statements)
    }

    fun addStatement(statement: JsStatement) {
        statements.add(statement)
    }

    inline fun <T> savingStatements(action: () -> T): T {
        val oldStatements = this.statements
        val result = action()
        this.statements = oldStatements
        return result
    }

    inline fun <T> withStatements(statements: MutableCollection<JsStatement>, action: () -> T): T = savingStatements {
        this.statements = statements
        action()
    }

    inline fun <T> nestedDeclaration(declaration: MemberDescriptor, action: () -> T): T {
        val oldDeclaration = this.currentDeclaration
        val oldClass = this.currentClass
        this.currentDeclaration = declaration
        if (declaration is ClassDescriptor) {
            this.currentClass = declaration
        }
        val result = action()
        this.currentDeclaration = oldDeclaration
        this.currentClass = oldClass
        return result
    }

    inline fun <T> nestedLoop(loop: IrLoop, action: () -> T): Pair<T, LoopState> {
        val state = LoopState()
        loops[loop] = state
        val result = action()
        loops.keys -= loop
        return Pair(result, state)
    }

    fun <T> withAliases(aliases: Collection<Pair<DeclarationDescriptor, JsExpression>>, action: () -> T): T {
        val backup = aliases.map { (descriptor, alias) ->
            val oldAlias = aliasesImpl[descriptor]
            aliasesImpl[descriptor] = alias
            Pair(descriptor, oldAlias)
        }
        val result = action()
        for ((descriptor, alias) in backup) {
            if (alias != null) {
                aliasesImpl[descriptor] = alias
            }
            else {
                aliasesImpl.remove(descriptor)
            }
        }
        return result
    }

    val isPublicInlineFunction: Boolean
        get() {
            var descriptor: DeclarationDescriptor? = currentDeclaration
            while (descriptor is FunctionDescriptor) {
                if (descriptor.isInline && descriptor.isEffectivelyPublicApi) {
                    return true
                }
                descriptor = descriptor.containingDeclaration
            }
            return false
        }

    fun getInnerReference(descriptor: DeclarationDescriptor): JsExpression = buildJs { naming.innerNames[descriptor].refPure() }
}