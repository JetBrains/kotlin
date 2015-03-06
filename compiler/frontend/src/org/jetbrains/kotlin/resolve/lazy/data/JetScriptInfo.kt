/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.data

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.ScriptNameUtil
import org.jetbrains.kotlin.name.FqName

public class JetScriptInfo(
        val script: JetScript
) : JetClassLikeInfo {
    public val fqName: FqName = ScriptNameUtil.classNameForScript(script)
    override fun getContainingPackageFqName() = fqName.parent()
    override fun getModifierList() = null
    override fun getDefaultObjects() = listOf<JetObjectDeclaration>()
    override fun getScopeAnchor() = script
    override fun getCorrespondingClassOrObject() = null
    override fun getTypeParameterList() = null
    override fun getPrimaryConstructorParameters() = listOf<JetParameter>()
    override fun getClassKind() = ClassKind.CLASS
    override fun getDeclarations() = script.getDeclarations()
            .filter(::shouldBeScriptClassMember)
    override fun getDanglingAnnotations() = listOf<JetAnnotationEntry>()
}

public fun shouldBeScriptClassMember(declaration: JetDeclaration): Boolean {
    // To avoid the necessity to always analyze the whole body of a script even if just its class descriptor is needed
    // we only add those vals, vars and funs that have explicitly specified return types
    // (or implicit Unit for function with block body)
    return declaration is JetCallableDeclaration && declaration.getTypeReference() != null
           || declaration is JetNamedFunction && declaration.hasBlockBody()
}
