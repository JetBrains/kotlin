/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;

/**
 * @author abreslav
 */
public abstract class KotlinTestWithEnvironmentManagement extends TestCase {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final Disposable rootDisposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    public class JetCoreEnvironmentWithDisposable {
        public final JetCoreEnvironment jetCoreEnvironment;

        public final Project project;

        public JetCoreEnvironmentWithDisposable(@NotNull ConfigurationKind configurationKind) {
            this.jetCoreEnvironment = JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(rootDisposable, configurationKind);
            this.project = jetCoreEnvironment.getProject();
        }

    }

    @Override
    public void tearDown() throws Exception {
        Disposer.dispose(rootDisposable);
        super.tearDown();
    }

}
