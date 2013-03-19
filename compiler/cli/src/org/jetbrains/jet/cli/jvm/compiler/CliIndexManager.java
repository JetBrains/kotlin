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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.collect.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.impl.PackageIndex;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.*;

public class CliIndexManager {

    public static CliIndexManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CliIndexManager.class);
    }

    private final NotNullLazyValue<Index> index;

    public CliIndexManager(@NotNull final JetCoreEnvironment jetCoreEnvironment) {
        this.index = new NotNullLazyValue<Index>() {
            @NotNull
            @Override
            protected Index compute() {
                return Index.compute(jetCoreEnvironment);
            }
        };        
    }

    @NotNull
    public Collection<JetFile> getPackageSources(@NotNull FqName packageFqName) {
        Collection<JetFile> files = index.getValue().packageToSources.get(packageFqName);
        if (files == null) {
            return Collections.emptyList();
        }
        return files;
    }

    public boolean packageExists(@NotNull FqName packageFqName) {
        return index.getValue().packageIndex.getAllPackages().contains(packageFqName);
    }

    @NotNull
    public Collection<FqName> getSubpackagesOf(@NotNull FqName parent) {
        return index.getValue().packageIndex.getSubPackagesOf(parent);
    }

    private static class Index {

        @NotNull
        public static Index compute(@NotNull JetCoreEnvironment jetCoreEnvironment) {
            List<JetFile> files = jetCoreEnvironment.getSourceFiles();
            Multimap<FqName, JetFile> packageToSources = HashMultimap.create();
            PackageIndex.Builder packageIndex = new PackageIndex.Builder();
            for (JetFile jetFile : files) {
                FqName fqName = JetPsiUtil.getFQName(jetFile);
                packageToSources.put(fqName, jetFile);
                packageIndex.addPackage(fqName);
            }
            return new Index(packageToSources.asMap(), packageIndex.build());
        }

        private final Map<FqName, Collection<JetFile>> packageToSources;
        private final PackageIndex packageIndex;

        private Index(
                @NotNull Map<FqName, Collection<JetFile>> packageToSources,
                @NotNull PackageIndex packageIndex
        ) {
            this.packageToSources = packageToSources;
            this.packageIndex = packageIndex;
        }

    }
}
