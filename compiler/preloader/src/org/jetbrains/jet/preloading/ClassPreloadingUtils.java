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

package org.jetbrains.jet.preloading;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassPreloadingUtils {

    public static abstract class ClassHandler {
        public byte[] instrument(String resourceName, byte[] data) {
            return data;
        }

        public void beforeDefineClass(String name, int sizeInBytes) {}
        public void afterDefineClass(String name) {}

        public void beforeLoadJar(File jarFile) {}
        public void afterLoadJar(File jarFile) {}
    }

    /**
     * Creates a class loader that loads all classes from {@code jarFiles} into memory to make loading faster (avoid skipping through zip archives).
     *
     * NOTE: if many resources with the same name exist, only the first one will be loaded
     *
     * @param jarFiles jars to load all classes from
     * @param classCountEstimation an estimated number of classes in a the jars
     * @param parentClassLoader parent class loader
     * @param handler handler to be notified on class definitions done by this class loader, or null
     * @param classesToLoadByParent condition to load some classes via parent class loader
     * @return a class loader that reads classes from memory
     * @throws IOException on from reading the jar
     */
    public static ClassLoader preloadClasses(
            Collection<File> jarFiles,
            int classCountEstimation,
            ClassLoader parentClassLoader,
            ClassCondition classesToLoadByParent,
            ClassHandler handler
    ) throws IOException {
        Map<String, ResourceData> entries = loadAllClassesFromJars(jarFiles, classCountEstimation, handler);

        return createMemoryBasedClassLoader(parentClassLoader, entries, handler, classesToLoadByParent);
    }

    public static ClassLoader preloadClasses(
            Collection<File> jarFiles, int classCountEstimation, ClassLoader parentClassLoader, ClassCondition classesToLoadByParent
    ) throws IOException {
        return preloadClasses(jarFiles, classCountEstimation, parentClassLoader, classesToLoadByParent, null);
    }

    private static ClassLoader createMemoryBasedClassLoader(
            final ClassLoader parent,
            final Map<String, ResourceData> preloadedResources,
            final ClassHandler handler,
            final ClassCondition classesToLoadByParent
    ) {
        return new ClassLoader(null) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (classesToLoadByParent != null && classesToLoadByParent.accept(name)) {
                    if (parent == null) {
                        return super.loadClass(name);
                    }

                    try {
                        return parent.loadClass(name);
                    }
                    catch (ClassNotFoundException e) {
                        return super.loadClass(name);
                    }
                }

                // Look in this class loader and then in the parent one
                Class<?> aClass = super.loadClass(name);
                if (aClass == null) {
                    if (parent == null) {
                        throw new ClassNotFoundException("Class not available in preloader: " + name);
                    }
                    return parent.loadClass(name);
                }
                return aClass;
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                String internalName = name.replace('.', '/').concat(".class");
                ResourceData resourceData = preloadedResources.get(internalName);
                if (resourceData == null) return null;

                int sizeInBytes = resourceData.bytes.length;
                if (handler != null) {
                    handler.beforeDefineClass(name, sizeInBytes);
                }

                Class<?> definedClass = defineClass(name, resourceData.bytes, 0, sizeInBytes);

                if (handler != null) {
                    handler.afterDefineClass(name);
                }

                return definedClass;
            }

            @Override
            public URL getResource(String name) {
                URL resource = super.getResource(name);
                if (resource == null && parent != null) {
                    return parent.getResource(name);
                }
                return resource;
            }

            @Override
            protected URL findResource(String name) {
                ResourceData resourceData = preloadedResources.get(name);
                if (resourceData == null) return null;
                return resourceData.getURL();
            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                Enumeration<URL> resources = super.getResources(name);
                if (!resources.hasMoreElements() && parent != null) {
                    return parent.getResources(name);
                }
                return resources;
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                URL resource = findResource(name);
                if (resource == null) {
                    return Collections.enumeration(Collections.<URL>emptyList());
                }
                // Only the first resource is loaded
                return Collections.enumeration(Collections.singletonList(resource));
            }
        };
    }

    private static Map<String, ResourceData> loadAllClassesFromJars(
            Collection<File> jarFiles,
            int classNumberEstimate,
            ClassHandler handler
    ) throws IOException {
        Map<String, ResourceData> resources = new HashMap<String, ResourceData>(classNumberEstimate);

        for (File jarFile : jarFiles) {
            if (handler != null) {
                handler.beforeLoadJar(jarFile);
            }

            FileInputStream fileInputStream = new FileInputStream(jarFile);
            try {
                byte[] buffer = new byte[10 * 1024];
                ZipInputStream stream = new ZipInputStream(new BufferedInputStream(fileInputStream));
                while (true) {
                    ZipEntry entry = stream.getNextEntry();
                    if (entry == null) break;
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (resources.containsKey(name)) continue; // Only the first resource is stored

                    int size = (int) entry.getSize();
                    int effectiveSize = size < 0 ? 32 : size;
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(effectiveSize);

                    int count;
                    while ((count = stream.read(buffer)) > 0) {
                        bytes.write(buffer, 0, count);
                    }

                    byte[] data = bytes.toByteArray();
                    if (handler != null) {
                        data = handler.instrument(name, data);
                    }

                    resources.put(name, new ResourceData(jarFile, name, data));
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

            if (handler != null) {
                handler.afterLoadJar(jarFile);
            }
        }
        return resources;
    }

    private static class ResourceData {
        private final File jarFile;
        private final String resourceName;
        private final byte[] bytes;

        public ResourceData(File jarFile, String resourceName, byte[] bytes) {
            this.jarFile = jarFile;
            this.resourceName = resourceName;
            this.bytes = bytes;
        }

        public URL getURL() {
            try {
                String path = "file:" + jarFile + "!/" + resourceName;
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

    }
}
