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

package org.jetbrains.jet.jvm.compiler;

import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.TypeTransformingVisitor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JdkAndAndroidDisparityTest extends UsefulTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TypeTransformingVisitor.setStrictMode(true);
    }

    @Override
    protected void tearDown() throws Exception {
        TypeTransformingVisitor.setStrictMode(false);
        super.tearDown();
    }

    public void testJdkAndAndroidDisparity() {
        List<FqName> jdkClasses = JdkAnnotationsValidityTest.getAffectedClasses("file://jdk-annotations");
        List<FqName> androidClasses = JdkAnnotationsValidityTest.getAffectedClasses("file://android-sdk-annotations");

        Set<FqName> sharedClasses = new HashSet<FqName>(jdkClasses);
        sharedClasses.retainAll(androidClasses);

        if (!sharedClasses.isEmpty()) {
            StringBuilder sb = new StringBuilder("Following classes are shared between JDK and Android SDK: \n");
            for (FqName fqName : sharedClasses) {
                sb.append(fqName.asString()).append("\n");
            }
            fail(sb.toString());
        }
    }
}
