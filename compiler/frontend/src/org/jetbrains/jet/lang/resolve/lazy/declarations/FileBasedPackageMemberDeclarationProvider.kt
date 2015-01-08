/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy.declarations

import kotlin.Function0
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager

public class FileBasedPackageMemberDeclarationProvider internal(
        storageManager: StorageManager,
        private val fqName: FqName,
        private val factory: FileBasedDeclarationProviderFactory,
        private val packageFiles: Collection<JetFile>)
: AbstractPsiBasedDeclarationProvider(storageManager), PackageMemberDeclarationProvider {

    private val allDeclaredSubPackages = storageManager.createLazyValue<Collection<FqName>> {
        factory.getAllDeclaredSubPackagesOf(fqName)
    }

    override fun doCreateIndex(index: AbstractPsiBasedDeclarationProvider.Index) {
        for (file in packageFiles) {
            for (declaration in file.getDeclarations()) {
                assert(fqName == file.getPackageFqName(), "Files declaration utils contains file with invalid package")
                index.putToIndex(declaration)
            }
        }
    }

    override fun getAllDeclaredSubPackages(): Collection<FqName> = allDeclaredSubPackages()

    override fun getPackageFiles() = packageFiles

    override fun toString() = "Declarations for package $fqName"
}
