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

import org.jetbrains.jet.ConfigurationKind;

public class MultiFileGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
    }

    public void testSimple() {
        blackBoxMultiFile("/multi/simple/box.kt", "/multi/simple/ok.kt");
    }

    public void testInternalVisibility() {
        blackBoxMultiFile("/multi/internalVisibility/box.kt", "/multi/internalVisibility/a.kt");
    }

    public void testNestedPackagesVisibility() {
        blackBoxMultiFile("/multi/nestedPackages/box.kt", "/multi/nestedPackages/a.kt");
    }

    public void testSameNames() {
        blackBoxMultiFile("/multi/same/1/box.kt", "/multi/same/2/box.kt");
    }

    public void testKt1515() {
        blackBoxMultiFile("/multi/kt1515/thisPackage.kt", "/multi/kt1515/otherPackage.kt");
    }
}
