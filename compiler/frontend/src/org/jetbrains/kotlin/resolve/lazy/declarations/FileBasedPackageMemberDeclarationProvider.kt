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

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

class FileBasedPackageMemberDeclarationProvider(
        storageManager: StorageManager,
        private val fqName: FqName,
        private val factory: FileBasedDeclarationProviderFactory,
        private val packageFiles: Collection<KtFile>)
: AbstractPsiBasedDeclarationProvider(storageManager), PackageMemberDeclarationProvider {

    private val allDeclaredSubPackages = storageManager.createLazyValue<Collection<FqName>> {
        factory.getAllDeclaredSubPackagesOf(fqName)
    }

    override fun doCreateIndex(index: AbstractPsiBasedDeclarationProvider.Index) {
        for (file in packageFiles) {
            for (declaration in file.declarations) {
                assert(fqName == file.packageFqName) { "Files declaration utils contains file with invalid package" }
                index.putToIndex(declaration)
            }
        }
    }

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> = allDeclaredSubPackages()

    override fun getPackageFiles() = packageFiles

    override fun toString() = "Declarations for package $fqName"
}
