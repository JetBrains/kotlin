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
import com.intellij.mock.MockApplication;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import org.picocontainer.MutablePicoContainer;

public abstract class KtPlatformLiteFixture extends KtUsefulTestCase {
    protected MockProjectEx myProject;

    public static MockApplication getApplication() {
        return (MockApplication) ApplicationManager.getApplication();
    }

    public void initApplication() {
        MockApplication instance = new MockApplication(getTestRootDisposable());
        ApplicationManager.setApplication(instance, FileTypeManager::getInstance, getTestRootDisposable());
        getApplication().registerService(EncodingManager.class, CoreEncodingProjectManager.class);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        clearFields(this);
        myProject = null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T registerComponentInstance(MutablePicoContainer container, Class<T> key, T implementation) {
        Object old = container.getComponentInstance(key);
        container.unregisterComponent(key);
        container.registerComponentInstance(key, implementation);
        return (T)old;
    }
}
