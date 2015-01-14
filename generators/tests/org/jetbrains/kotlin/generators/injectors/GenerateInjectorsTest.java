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

package org.jetbrains.kotlin.generators.injectors;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.kotlin.generators.di.DependencyInjectorGenerator;
import org.junit.Assert;

import java.io.IOException;

@SuppressWarnings({"JUnitTestCaseWithNoTests", "JUnitTestCaseWithNonTrivialConstructors"})
public class GenerateInjectorsTest extends TestCase {

    private final DependencyInjectorGenerator generator;

    public GenerateInjectorsTest(DependencyInjectorGenerator generator) {
        super(generator.getInjectorClassName());
        this.generator = generator;
    }

    @Override
    protected void runTest() throws Throwable {
        CharSequence text = generator.generateText();
        String expected = FileUtil.loadFile(generator.getOutputFile(), true);
        String expectedText = StringUtil.convertLineSeparators(expected.trim());
        String actualText = StringUtil.convertLineSeparators(text.toString().trim());
        Assert.assertEquals("To fix this problem you need to run GenerateInjectors", expectedText, actualText);
    }

    public static TestSuite suite() throws IOException {
        TestSuite suite = new TestSuite();
        for (DependencyInjectorGenerator generator : InjectorsPackage.createInjectorGenerators()) {
            suite.addTest(new GenerateInjectorsTest(generator));
        }
        return suite;
    }
}
