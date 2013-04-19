package org.jetbrains.jet.preloading;/*
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

import sun.misc.CompoundEnumeration;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassPreloadingUtils {

    /**
     * Creates a class loader that loads all classes from {@code jarFile} into memory to make loading faster (avoid skipping through zip archives).
     * @param jarFile a jar to load all classes from
     * @param classCountEstimation an estimated number of classes in a the jar
     * @param parent (nullable) parent class loader
     * @return a class loader that reads classes from memory
     * @throws IOException on from reading the jar
     */
    public static ClassLoader preloadClasses(File jarFile, int classCountEstimation, ClassLoader parent) throws IOException {
        Map<String, byte[]> entries = loadAllClassesFromJar(jarFile, classCountEstimation);

        return createMemoryBasedClassLoader(parent, jarFile, entries);
    }

    private static ClassLoader createMemoryBasedClassLoader(
            final ClassLoader parent,
            final File jarFile,
            final Map<String, byte[]> preloadedResources
    ) {
        return new ClassLoader(null) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                // Look in this class loader and then in the parent one
                Class<?> aClass = super.loadClass(name);
                if (aClass == null) {
                    return parent.loadClass(name);
                }
                return aClass;
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                String internalName = name.replace('.', '/').concat(".class");
                byte[] bytes = preloadedResources.get(internalName);
                if (bytes == null) return null;

                return defineClass(name, bytes, 0, bytes.length);
            }

            @Override
            protected URL findResource(String name) {
                final byte[] bytes = preloadedResources.get(name);
                if (bytes == null) return null;

                try {
                    String path = "file:" + jarFile + "!" + name;
                    return new URL("jar", null, 0, path, new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            return new URLConnection(u) {
                                @Override
                                public void connect() throws IOException {}

                                @Override
                                public InputStream getInputStream() throws IOException {
                                    return new ByteArrayInputStream(bytes);
                                }
                            };
                        }
                    });
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                URL resource = findResource(name);
                if (resource == null) {
                    return new CompoundEnumeration<URL>(new Enumeration[0]);
                }
                return Collections.enumeration(Collections.singletonList(resource));
            }
        };
    }

    private static Map<String, byte[]> loadAllClassesFromJar(File jarFile, int classNumberEstimate) throws IOException {
        Map<String, byte[]> classes = new HashMap<String, byte[]>(classNumberEstimate);

        FileInputStream fileInputStream = new FileInputStream(jarFile);
        try {
            byte[] buffer = new byte[10 * 1024];
            ZipInputStream stream = new ZipInputStream(new BufferedInputStream(fileInputStream));
            while (true) {
                ZipEntry entry = stream.getNextEntry();
                if (entry == null) break;

                ByteArrayOutputStreamWithPublicArray bytes = new ByteArrayOutputStreamWithPublicArray((int) entry.getSize());
                int count;
                while ((count = stream.read(buffer)) > 0) {
                  bytes.write(buffer, 0, count);
                }
                if (!entry.isDirectory()) {
                    classes.put(entry.getName(), bytes.getBytes());
                }
            }
        }
        finally {
            try {
                fileInputStream.close();
            }
            catch (IOException e) {
                // Ignore
            }
        }
        return classes;
    }

    private static class ByteArrayOutputStreamWithPublicArray extends ByteArrayOutputStream {
        public ByteArrayOutputStreamWithPublicArray(int size) {
            super(size);
        }

        // To avoid copying the array
        public byte[] getBytes() {
            return buf;
        }
    }
}
