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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

class ImportsProvider {
    private final List<JetImportDirective> importDirectives;

    private ListMultimap<Name, JetImportDirective> nameToDirectives = null;
    private List<JetImportDirective> allUnderImports = null;
    private boolean indexed;

    public ImportsProvider(List<JetImportDirective> importDirectives) {
        this.importDirectives = importDirectives;
    }

    @NotNull
    public List<JetImportDirective> getImports(@NotNull Name name) {
        createIndex();
        return nameToDirectives.containsKey(name) ? nameToDirectives.get(name) : allUnderImports;
    }

    @NotNull
    public List<JetImportDirective> getAllImports() {
        return importDirectives;
    }

    private void createIndex() {
        if (indexed) {
            return;
        }

        ImmutableListMultimap.Builder<Name, JetImportDirective> namesToRelativeImportsBuilder = ImmutableListMultimap.builder();

        Set<Name> processedAliases = Sets.newHashSet();
        List<JetImportDirective> processedAllUnderImports = Lists.newArrayList();

        for (JetImportDirective anImport : importDirectives) {
            ImportPath path = JetPsiUtil.getImportPath(anImport);
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

        allUnderImports = ImmutableList.copyOf(processedAllUnderImports);
        nameToDirectives = namesToRelativeImportsBuilder.build();

        indexed = true;
    }
}
