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

package org.jetbrains.jet.lang.resolve.android;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.OutputFile;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ByteArrayClassLoader extends URLClassLoader {

    private final Queue<OutputFile> q;
    private final Map<String, Class> extraClassDefs = new HashMap<String, Class>();

    @NotNull
    @Override
    protected Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
        Class aClass = extraClassDefs.get(name);
        if (aClass != null) return aClass;
        else return super.findClass(name);
    }

    public ByteArrayClassLoader(URL[] urls, ClassLoader parent, List<OutputFile> files) {
        super(urls, parent);
        q = new LinkedList<OutputFile>(files);
    }

    private void loadBytes(OutputFile f) {
        try {
            byte[] b = f.asByteArray();
            String name = relPathToClassName(f.getRelativePath());
            Class<?> aClass = defineClass(name, b, 0, b.length);
            extraClassDefs.put(name, aClass);
            q.remove(f);
        } catch (NoClassDefFoundError e) {
            OutputFile found = findByClassName(e.getMessage());
            if (found == null) throw e;
            else {
                loadBytes(found);
                loadBytes(f);
            }
        }
    }

    public void loadFiles() {
        OutputFile f = q.peek();
        while (f != null) {
            loadBytes(f);
            f = q.peek();
        }
    }

    private OutputFile findByClassName(String name) {
        String path = classNameToRelPath(name);
        for (OutputFile file: q)
            if (file.getRelativePath().equals(path)) return file;
        return null;
    }

    private static String relPathToClassName(String path) {
        return path.replace(".class", "").replace("/", ".");
    }

    private static String classNameToRelPath(String name) {
        return name.replace(".", "/").concat(".class");
    }
}
