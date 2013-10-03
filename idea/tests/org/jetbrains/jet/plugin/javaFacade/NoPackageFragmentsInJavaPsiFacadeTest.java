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

package org.jetbrains.jet.plugin.javaFacade;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;

import java.util.Set;
import java.util.TreeSet;

public class NoPackageFragmentsInJavaPsiFacadeTest extends LightCodeInsightFixtureTestCase {
    public void testPackageFragmentsAreNotVisible() {
        PsiPackage psiPackage = myFixture.findPackage("kotlin");

        Set<String> packageFragments = new TreeSet<String>();
        for (PsiClass psiClass : psiPackage.getClasses()) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName != null && qualifiedName.contains("-")) {
                packageFragments.add(qualifiedName);
            }
        }

        assertEmpty("Package fragment classes should not be found via JavaPsiFacade:\n" + packageFragments, packageFragments);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }
}
