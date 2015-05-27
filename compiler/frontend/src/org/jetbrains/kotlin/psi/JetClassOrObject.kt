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

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.name.Name

public interface JetClassOrObject : PsiNameIdentifierOwner, JetDeclarationContainer, JetElement, JetModifierListOwner, JetNamedDeclaration {
    public fun getDelegationSpecifierList(): JetDelegationSpecifierList?

    public fun getDelegationSpecifiers(): List<JetDelegationSpecifier>

    public fun getAnonymousInitializers(): List<JetClassInitializer>

    override fun getNameAsName(): Name?

    override fun getModifierList(): JetModifierList?

    public fun getNameAsDeclaration(): JetObjectDeclarationName?

    public fun getBody(): JetClassBody?

    public fun isTopLevel(): Boolean

    public fun isLocal(): Boolean
}
