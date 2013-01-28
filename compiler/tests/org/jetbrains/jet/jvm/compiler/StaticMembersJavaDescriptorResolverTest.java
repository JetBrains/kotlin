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

import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.File;
import java.io.IOException;

import static org.jetbrains.jet.test.util.NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaceWithFile;

public final class StaticMembersJavaDescriptorResolverTest extends AbstractJavaResolverDescriptorTest {

    private static final String PATH = "compiler/testData/javaDescriptorResolver/staticMembers/";
    private static final String DEFAULT_PACKAGE = "test";

    @NotNull
    @Override
    protected String getPath() {
        return PATH;
    }

    public void testSimple() throws Exception {
        doTest();
    }

    public void testInnerClass() throws Exception {
        doTest();
    }

    public void testDeeplyInnerClass() throws Exception {
        doTest();
    }

    public void testEnum() throws Exception {
        doTest();
    }

    private void doTest() throws IOException {
        String name = getTestName(false);
        compileJavaFile(name + ".java");
        NamespaceDescriptor namespaceWithClass = javaDescriptorResolver.resolveNamespace(new FqName(DEFAULT_PACKAGE));
        Assert.assertNotNull(namespaceWithClass);
        compareNamespaceWithFile(namespaceWithClass, DONT_INCLUDE_METHODS_OF_OBJECT, new File(PATH + name + "_non_static.txt"));

        NamespaceDescriptor namespaceWithStaticMembers = javaDescriptorResolver.resolveNamespace(new FqName(DEFAULT_PACKAGE + "." + name));
        File fileWithStaticMembers = new File(PATH + name + "_static.txt");
        if (namespaceWithStaticMembers == null) {
            Assert.assertTrue(!fileWithStaticMembers.exists());
        } else {
            compareNamespaceWithFile(namespaceWithStaticMembers, DONT_INCLUDE_METHODS_OF_OBJECT, fileWithStaticMembers);
        }
    }
}
