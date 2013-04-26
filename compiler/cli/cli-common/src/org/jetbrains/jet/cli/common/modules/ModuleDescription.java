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

package org.jetbrains.jet.cli.common.modules;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This interface duplicates {@link jet.modules.Module}, because cli-common should not depend on kotlin-runtime.jar
 */
public interface ModuleDescription {

    @NotNull
    String getModuleName();

    @NotNull
    List<String> getSourceFiles();

    @NotNull
    List<String> getClasspathRoots();

    @NotNull
    List<String> getAnnotationsRoots();

    class Impl implements ModuleDescription {

        private String name;
        private final List<String> sources = new SmartList<String>();
        private final List<String> classpath = new SmartList<String>();
        private final List<String> annotations = new SmartList<String>();

        public void setName(String name) {
            this.name = name;
        }

        public void addSourcePath(String path) {
            sources.add(path);
        }

        public void addClassPath(String path) {
            classpath.add(path);
        }

        public void addAnnotationPath(String path) {
            annotations.add(path);
        }

        @NotNull
        @Override
        public String getModuleName() {
            return name;
        }

        @NotNull
        @Override
        public List<String> getSourceFiles() {
            return sources;
        }

        @NotNull
        @Override
        public List<String> getClasspathRoots() {
            return classpath;
        }

        @NotNull
        @Override
        public List<String> getAnnotationsRoots() {
            return annotations;
        }

        @Override
        public String toString() {
            return name + "\n\tsources=" + sources + "\n\tclasspath=" + classpath + "\n\tannotations=" + annotations;
        }
    }
}
