/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.*;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;

import java.util.List;
import java.util.Set;

class ImportsProvider {
    private final List<JetImportDirective> importDirectives;
    private final NotNullLazyValue<NameToImportsCache> importsCacheValue;

    public ImportsProvider(StorageManager storageManager, final List<JetImportDirective> importDirectives) {
        this.importDirectives = importDirectives;
        this.importsCacheValue = storageManager.createLazyValue(new Function0<NameToImportsCache>() {
            @Override
            public NameToImportsCache invoke() {
                return NameToImportsCache.createIndex(importDirectives);
            }
        });
    }

    @NotNull
    public List<JetImportDirective> getImports(@NotNull Name name) {
        return importsCacheValue.invoke().getImports(name);
    }

    @NotNull
    public List<JetImportDirective> getAllImports() {
        return importDirectives;
    }

    private static class NameToImportsCache {
        private final ListMultimap<Name, JetImportDirective> nameToDirectives;
        private final List<JetImportDirective> allUnderImports;

        private NameToImportsCache(ListMultimap<Name, JetImportDirective> directives, List<JetImportDirective> imports) {
            nameToDirectives = directives;
            allUnderImports = imports;
        }

        private List<JetImportDirective> getImports(@NotNull Name name) {
            return nameToDirectives.containsKey(name) ? nameToDirectives.get(name) : allUnderImports;
        }

        private static NameToImportsCache createIndex(List<JetImportDirective> importDirectives) {
            ImmutableListMultimap.Builder<Name, JetImportDirective> namesToRelativeImportsBuilder = ImmutableListMultimap.builder();

            Set<Name> processedAliases = Sets.newHashSet();
            List<JetImportDirective> processedAllUnderImports = Lists.newArrayList();

            for (JetImportDirective anImport : importDirectives) {
                ImportPath path = anImport.getImportPath();
                if (path == null) {
                    // Could be some parse errors
                    continue;
                }

                if (path.isAllUnder()) {
                    processedAllUnderImports.add(anImport);

                    // All-Under import is relevant to all names found so far
                    for (Name aliasName : processedAliases) {
                        namesToRelativeImportsBuilder.put(aliasName, anImport);
                    }
                }
                else {
                    Name aliasName = path.getImportedName();
                    assert aliasName != null;

                    if (!processedAliases.contains(aliasName)) {
                        processedAliases.add(aliasName);

                        // Add to relevant imports all all-under imports found by this moment
                        namesToRelativeImportsBuilder.putAll(aliasName, processedAllUnderImports);
                    }

                    namesToRelativeImportsBuilder.put(aliasName, anImport);
                }
            }

            return new NameToImportsCache(namesToRelativeImportsBuilder.build(), ImmutableList.copyOf(processedAllUnderImports));
        }
    }
}
