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

package org.jetbrains.kotlin.resolve.lazy.declarations

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo

interface ClassMemberDeclarationProvider : DeclarationProvider {
    val ownerInfo: KtClassLikeInfo? // is null for synthetic classes/object that don't present in the source code

    val correspondingClassOrObject: KtPureClassOrObject? get() = ownerInfo?.correspondingClassOrObject
    val primaryConstructorParameters: List<KtParameter> get() = ownerInfo?.primaryConstructorParameters ?: emptyList()
    val companionObjects: List<KtObjectDeclaration> get() = ownerInfo?.companionObjects ?: emptyList()
}
