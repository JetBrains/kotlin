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

package org.jetbrains.kotlin.preloading;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("unchecked")
public class ClassPreloadingUtils {
    /**
     * Creates a class loader that loads all classes from {@code jarFiles} into memory to make loading faster (avoid skipping through zip archives).
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
        Map<String, Object> entries = loadAllClassesFromJars(jarFiles, classCountEstimation, handler);

        Collection<File> classpath = mergeClasspathFromManifests(entries);
        if (!classpath.isEmpty()) {
            parentClassLoader = preloadClasses(classpath, classCountEstimation, parentClassLoader, null, handler);
        }

        return new MemoryBasedClassLoader(classesToLoadByParent, parentClassLoader, entries, handler, createFallbackClassLoader(jarFiles));
    }

    private static URLClassLoader createFallbackClassLoader(Collection<File> files) throws IOException {
        List<URL> urls = new ArrayList<URL>(files.size());
        for (File file : files) {
            urls.add(file.toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
    }

    public static ClassLoader preloadClasses(
            Collection<File> jarFiles, int classCountEstimation, ClassLoader parentClassLoader, ClassCondition classesToLoadByParent
    ) throws IOException {
        return preloadClasses(jarFiles, classCountEstimation, parentClassLoader, classesToLoadByParent, null);
    }

    private static Collection<File> mergeClasspathFromManifests(Map<String, Object> preloadedResources) throws IOException {
        Object manifest = preloadedResources.get(JarFile.MANIFEST_NAME);
        if (manifest instanceof ResourceData) {
            return extractManifestClasspath((ResourceData) manifest);
        }
        else if (manifest instanceof ArrayList) {
            List<File> result = new ArrayList<File>();
            for (ResourceData data : (ArrayList<ResourceData>) manifest) {
                result.addAll(extractManifestClasspath(data));
            }
            return result;
        }
        else {
            assert manifest == null : "Resource map should contain ResourceData or ArrayList<ResourceData>: " + manifest;
            return Collections.emptyList();
        }
    }

    private static Collection<File> extractManifestClasspath(ResourceData manifestData) throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(manifestData.bytes));
        String classpathSpaceSeparated = (String) manifest.getMainAttributes().get(Attributes.Name.CLASS_PATH);
        if (classpathSpaceSeparated == null) return Collections.emptyList();

        Collection<File> classpath = new ArrayList<File>(1);
        for (String jar : classpathSpaceSeparated.split(" ")) {
            if (".".equals(jar)) continue;

            if (!jar.endsWith(".jar")) {
                throw new UnsupportedOperationException("Class-Path attribute should only contain paths to JAR files: " + jar);
            }

            classpath.add(new File(manifestData.jarFile.getParent(), jar));
        }

        return classpath;
    }

    /**
     * @return a map of name to resources. Each value is either a ResourceData if there's only one instance (in the vast majority of cases)
     * or a non-empty ArrayList of ResourceData if there's many
     */
    private static Map<String, Object> loadAllClassesFromJars(
            Collection<File> jarFiles,
            int classNumberEstimate,
            ClassHandler handler
    ) throws IOException {
        // 0.75 is HashMap.DEFAULT_LOAD_FACTOR
        Map<String, Object> resources = new HashMap<String, Object>((int) (classNumberEstimate / 0.75));

        for (File jarFile : jarFiles) {
            if (handler != null) {
                handler.beforeLoadJar(jarFile);
            }

            FileInputStream fileInputStream = new FileInputStream(jarFile);
            try {
                byte[] buffer = new byte[10 * 1024];
                ZipInputStream stream = new ZipInputStream(new BufferedInputStream(fileInputStream, 1 << 19));
                while (true) {
                    ZipEntry entry = stream.getNextEntry();
                    if (entry == null) break;
                    if (entry.isDirectory()) continue;

                    int size = (int) entry.getSize();
                    int effectiveSize = size < 0 ? 32 : size;
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(effectiveSize);

                    int count;
                    while ((count = stream.read(buffer)) > 0) {
                        bytes.write(buffer, 0, count);
                    }

                    String name = entry.getName();
                    byte[] data = bytes.toByteArray();
                    if (handler != null) {
                        data = handler.instrument(name, data);
                    }
                    ResourceData resourceData = new ResourceData(jarFile, name, data);

                    Object previous = resources.get(name);
                    if (previous == null) {
                        resources.put(name, resourceData);
                    }
                    else if (previous instanceof ResourceData) {
                        List<ResourceData> list = new ArrayList<ResourceData>();
                        list.add((ResourceData) previous);
                        list.add(resourceData);
                        resources.put(name, list);
                    }
                    else {
                        assert previous instanceof ArrayList :
                                "Resource map should contain ResourceData or ArrayList<ResourceData>: " + name;
                        ((ArrayList<ResourceData>) previous).add(resourceData);
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

            if (handler != null) {
                handler.afterLoadJar(jarFile);
            }
        }

        for (Object value : resources.values()) {
            if (value instanceof ArrayList) {
                ((ArrayList) value).trimToSize();
            }
        }

        return resources;
    }
}
