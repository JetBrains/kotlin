/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.core.CoreEncodingProjectManager;
import com.intellij.mock.MockApplicationEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import org.picocontainer.MutablePicoContainer;

import java.lang.reflect.Modifier;

public abstract class KtPlatformLiteFixture extends KtUsefulTestCase {
    protected MockProjectEx myProject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Extensions.cleanRootArea(getTestRootDisposable());
    }

    public static MockApplicationEx getApplication() {
        return (MockApplicationEx)ApplicationManager.getApplication();
    }

    public void initApplication() {
        MockApplicationEx instance = new MockApplicationEx(getTestRootDisposable());
        ApplicationManager.setApplication(instance, FileTypeManager::getInstance, getTestRootDisposable());
        getApplication().registerService(EncodingManager.class, CoreEncodingProjectManager.class);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        clearFields(this);
        myProject = null;
    }

    protected <T> void registerExtensionPoint(ExtensionPointName<T> extensionPointName, Class<T> aClass) {
        registerExtensionPoint(Extensions.getRootArea(), extensionPointName, aClass);
    }

    private static <T> void registerExtensionPoint(
            ExtensionsArea area, ExtensionPointName<T> extensionPointName,
            Class<? extends T> aClass
    ) {
        String name = extensionPointName.getName();
        if (!area.hasExtensionPoint(name)) {
            ExtensionPoint.Kind kind = aClass.isInterface() || (aClass.getModifiers() & Modifier.ABSTRACT) != 0 ? ExtensionPoint.Kind.INTERFACE : ExtensionPoint.Kind.BEAN_CLASS;
            area.registerExtensionPoint(name, aClass.getName(), kind);
        }
    }

    public static <T> T registerComponentInstance(MutablePicoContainer container, Class<T> key, T implementation) {
        Object old = container.getComponentInstance(key);
        container.unregisterComponent(key);
        container.registerComponentInstance(key, implementation);
        //noinspection unchecked
        return (T)old;
    }
}
