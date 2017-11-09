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

import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.storage.StorageManager

class PsiBasedClassMemberDeclarationProvider(
        storageManager: StorageManager,
        override val ownerInfo: KtClassLikeInfo)
: AbstractPsiBasedDeclarationProvider(storageManager), ClassMemberDeclarationProvider {

    override fun doCreateIndex(index: AbstractPsiBasedDeclarationProvider.Index) {
        for (declaration in ownerInfo.declarations) {
            index.putToIndex(declaration)
        }

        for (parameter in ownerInfo.primaryConstructorParameters) {
            if (parameter.hasValOrVar()) {
                index.putToIndex(parameter)
            }
        }
    }

    override fun toString() = "Declarations for $ownerInfo"
}
