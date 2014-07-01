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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.OutputFile;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.Manifest;

public class GeneratedClassLoader extends URLClassLoader {
    private ClassFileFactory state;

    public GeneratedClassLoader(@NotNull ClassFileFactory state, ClassLoader parentClassLoader, URL...urls) {
        super(urls, parentClassLoader);
        this.state = state;
    }

    @NotNull
    @Override
    protected Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
        String classFilePath = name.replace('.', '/') + ".class";

        OutputFile outputFile = state.get(classFilePath);
        if (outputFile != null) {
            byte[] bytes = outputFile.asByteArray();
            int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) {
                String pkgName = name.substring(0, lastDot);
                if (getPackage(pkgName) == null) {
                    definePackage(pkgName, new Manifest(), null);
                }
            }
            return defineClass(name, bytes, 0, bytes.length);
        }

        return super.findClass(name);
    }

    public void dispose() {
        state = null;
    }

    @NotNull
    public List<OutputFile> getAllGeneratedFiles() {
        return state.asList();
    }
}
