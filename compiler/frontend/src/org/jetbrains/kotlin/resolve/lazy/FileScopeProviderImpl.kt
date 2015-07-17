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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.storage.get

public class FileScopeProviderImpl(
        private val resolveSession: ResolveSession,
        private val additionalScopes: Iterable<FileScopeProvider.AdditionalScopes>
) : FileScopeProvider {

    private val defaultImports by resolveSession.getStorageManager().createLazyValue {
        val defaultImports = resolveSession.getModuleDescriptor().defaultImports
        resolveSession.getJetImportsFactory().createImportDirectives(defaultImports)
    }

    private val fileScopes = resolveSession.getStorageManager().createMemoizedFunction { file: JetFile -> createFileScope(file) }

    override fun getFileScope(file: JetFile) = fileScopes(file)

    private fun createFileScope(file: JetFile): LazyFileScope {
        val tempTrace = TemporaryBindingTrace.create(resolveSession.getTrace(), "Transient trace for default imports lazy resolve")
        return LazyFileScope.create(
                resolveSession, file, defaultImports, additionalScopes.flatMap { it.scopes(file) },
                resolveSession.getTrace(), tempTrace, "LazyFileScope for file " + file.getName()
        )
    }
}
