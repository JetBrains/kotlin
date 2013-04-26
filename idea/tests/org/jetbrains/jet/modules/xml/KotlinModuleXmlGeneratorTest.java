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

package org.jetbrains.jet.modules.xml;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.runner.KotlinModuleDescriptionGenerator;
import org.jetbrains.jet.compiler.runner.KotlinModuleXmlGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class KotlinModuleXmlGeneratorTest extends TestCase {
    public void testBasic() throws Exception {
        String actual = KotlinModuleXmlGenerator.INSTANCE.generateModuleScript(
                "name",
                new KotlinModuleDescriptionGenerator.DependencyProvider() {
                    @Override
                    public void processClassPath(@NotNull KotlinModuleDescriptionGenerator.DependencyProcessor processor) {
                        processor.processAnnotationRoots(Arrays.asList(new File("a1/f1"), new File("a2")));
                        processor.processClassPathSection("s1", Arrays.asList(new File("cp1"), new File("cp2")));
                    }
                },
                Arrays.asList(new File("s1"), new File("s2")),
                false,
                Collections.<File>emptySet()
        ).toString();
        String expected = FileUtil.loadFile(new File("idea/testData/modules.xml/basic.xml"));
        assertEquals(expected, actual);
    }

    public void testFiltered() throws Exception {
        String actual = KotlinModuleXmlGenerator.INSTANCE.generateModuleScript(
                "name",
                new KotlinModuleDescriptionGenerator.DependencyProvider() {
                    @Override
                    public void processClassPath(@NotNull KotlinModuleDescriptionGenerator.DependencyProcessor processor) {
                        processor.processAnnotationRoots(Arrays.asList(new File("a1/f1"), new File("a2")));
                        processor.processClassPathSection("s1", Arrays.asList(new File("cp1"), new File("cp2")));
                    }
                },
                Arrays.asList(new File("s1"), new File("s2")),
                false,
                Collections.singleton(new File("cp1"))
        ).toString();
        String expected = FileUtil.loadFile(new File("idea/testData/modules.xml/filtered.xml"));
        assertEquals(expected, actual);
    }
}
