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

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A class loader which loads classes and resources from the given map.
 *
 * To save memory, as soon as any class is loaded, its bytecode is removed from the map.
 * This means that once any class is loaded, it _cannot be found_ as a resource anymore.
 * Therefore if you need to be able to find classes via findResource(), you should pass a fallback
 * class loader which is able to do that at any point of time.
 */
@SuppressWarnings("unchecked")
public class MemoryBasedClassLoader extends ClassLoader {
    private final ClassCondition classesToLoadByParent;
    private final ClassLoader parent;
    private final Map<String, Object> preloadedResources;
    private final ClassHandler handler;
    private final ClassLoader fallbackResourceLoader;

    public MemoryBasedClassLoader(
            ClassCondition classesToLoadByParent,
            ClassLoader parent,
            Map<String, Object> preloadedResources,
            ClassHandler handler,
            ClassLoader fallbackResourceLoader
    ) {
        super(null);
        this.classesToLoadByParent = classesToLoadByParent;
        this.parent = parent;
        this.preloadedResources = preloadedResources;
        this.handler = handler;
        this.fallbackResourceLoader = fallbackResourceLoader;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (classesToLoadByParent != null && classesToLoadByParent.accept(name)) {
            if (parent == null) {
                return super.loadClass(name, resolve);
            }

            try {
                return parent.loadClass(name);
            }
            catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }

        // Look in this class loader and then in the parent one
        Class<?> aClass = super.loadClass(name, resolve);
        if (aClass == null) {
            if (parent == null) {
                throw new ClassNotFoundException("Class not available in preloader: " + name);
            }
            return parent.loadClass(name);
        }
        return aClass;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internalName = name.replace('.', '/').concat(".class");
        Object resources = preloadedResources.get(internalName);
        if (resources == null) return null;

        // Clear the resource, we won't need it anymore
        preloadedResources.remove(internalName);

        ResourceData resourceData = resources instanceof ResourceData
                                    ? ((ResourceData) resources)
                                    : ((List<ResourceData>) resources).get(0);

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
        if (resource == null) {
            resource = fallbackResourceLoader.getResource(name);
        }

        if (resource == null && parent != null) {
            return parent.getResource(name);
        }
        return resource;
    }

    @Override
    protected URL findResource(String name) {
        Enumeration<URL> resources = findResources(name);
        return resources.hasMoreElements() ? resources.nextElement() : null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> own = super.getResources(name);
        if (!own.hasMoreElements()) {
            own = fallbackResourceLoader.getResources(name);
        }

        if (parent == null) return own;

        Enumeration<URL> fromParent = parent.getResources(name);
        if (!own.hasMoreElements()) return fromParent;

        List<URL> result = new ArrayList<URL>();
        while (own.hasMoreElements()) {
            result.add(own.nextElement());
        }
        while (fromParent.hasMoreElements()) {
            result.add(fromParent.nextElement());
        }

        return Collections.enumeration(result);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        Object resources = preloadedResources.get(name);
        if (resources == null) {
            return Collections.enumeration(Collections.<URL>emptyList());
        }
        else if (resources instanceof ResourceData) {
            return Collections.enumeration(Collections.singletonList(((ResourceData) resources).getURL()));
        }
        else {
            assert resources instanceof ArrayList : "Resource map should contain ResourceData or ArrayList<ResourceData>: " + name;
            List<ResourceData> resourceDatas = (ArrayList<ResourceData>) resources;
            List<URL> urls = new ArrayList<URL>(resourceDatas.size());
            for (ResourceData data : resourceDatas) {
                urls.add(data.getURL());
            }
            return Collections.enumeration(urls);
        }
    }
}
