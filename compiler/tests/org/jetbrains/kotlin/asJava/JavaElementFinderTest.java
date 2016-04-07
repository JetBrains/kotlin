/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class JavaElementFinderTest extends KotlinAsJavaTestBase {

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Collections.singletonList(
                new File("compiler/testData/asJava/findClasses/" + getTestName(false) + ".kt")
        );
    }

    @Override
    protected void tearDown() throws Exception {
        finder = null;
        super.tearDown();
    }

    public void testFromEnumEntry() {
        assertClass("Direction");
        assertNoClass("Direction.NORTH");
        assertNoClass("Direction.SOUTH");
        assertNoClass("Direction.WEST");
        // TODO: assertClass("Direction.SOUTH.Hello");
        // TODO: assertClass("Direction.WEST.Some");
    }

    public void testEmptyQualifiedName() {
        assertNoClass("");
    }

    private void assertClass(String qualifiedName) {
        PsiClass psiClass = finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
        TestCase.assertNotNull(String.format("Class with fqn='%s' wasn't found.", qualifiedName), psiClass);
        TestCase.assertTrue(String.format("Class with fqn='%s' is not valid.", qualifiedName), psiClass.isValid());
    }

    private void assertNoClass(String qualifiedName) {
        TestCase.assertNull(String.format("Class with fqn='%s' isn't expected to be found.", qualifiedName),
                            finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject())));
    }
}
