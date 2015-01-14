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

package org.jetbrains.kotlin.resolve.lazy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import kotlin.Function0;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetImportDirective;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;

import java.util.Collections;
import java.util.List;

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
    public List<JetImportDirective> getImports(@NotNull Name name, @NotNull LookupMode mode) {
        return importsCacheValue.invoke().getImports(name, mode);
    }

    @NotNull
    public List<JetImportDirective> getAllImports() {
        return importDirectives;
    }

    public enum LookupMode {
        CLASS,
        PACKAGE,
        FUNCTION_OR_PROPERTY
    }

    private static class NameToImportsCache {
        private final ListMultimap<Name, JetImportDirective> nameToDirectives;
        private final List<JetImportDirective> allUnderImports;

        private NameToImportsCache(ListMultimap<Name, JetImportDirective> directives, List<JetImportDirective> imports) {
            nameToDirectives = directives;
            allUnderImports = imports;
        }

        private List<JetImportDirective> getImports(@NotNull Name name, @NotNull LookupMode mode) {
            switch (mode) {
                 case CLASS:
                     return nameToDirectives.containsKey(name) ? nameToDirectives.get(name) : allUnderImports; // for class lookup explicit imports have priority

                 case PACKAGE:
                     return nameToDirectives.containsKey(name) ? nameToDirectives.get(name) : Collections.<JetImportDirective>emptyList(); // no packages import by all under imports

                 case FUNCTION_OR_PROPERTY:
                     return nameToDirectives.containsKey(name) ? KotlinPackage.plus(nameToDirectives.get(name), allUnderImports) : allUnderImports;

                default:
                    throw new IllegalArgumentException();
            }
        }

        private static NameToImportsCache createIndex(List<JetImportDirective> importDirectives) {
            ImmutableListMultimap.Builder<Name, JetImportDirective> namesToRelativeImportsBuilder = ImmutableListMultimap.builder();

            List<JetImportDirective> allUnderImports = Lists.newArrayList();

            for (JetImportDirective anImport : importDirectives) {
                ImportPath path = anImport.getImportPath();
                if (path == null) continue; // Could be some parse errors

                if (path.isAllUnder()) {
                    allUnderImports.add(anImport);
                }
                else {
                    Name aliasName = path.getImportedName();
                    assert aliasName != null;
                    namesToRelativeImportsBuilder.put(aliasName, anImport);
                }
            }

            return new NameToImportsCache(namesToRelativeImportsBuilder.build(), ImmutableList.copyOf(allUnderImports));
        }
    }
}
