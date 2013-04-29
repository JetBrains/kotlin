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
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.common.messages.MessageRenderer;
import org.jetbrains.jet.cli.common.modules.ModuleDescription;
import org.jetbrains.jet.cli.common.modules.ModuleXmlParser;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class AbstractModuleXmlParserTest extends TestCase {

    protected void doTest(String xmlPath) throws IOException {
        File txtFile = new File(FileUtil.getNameWithoutExtension(xmlPath) + ".txt");

        List<ModuleDescription> result = ModuleXmlParser.parse(xmlPath, new MessageCollector() {
            @Override
            public void report(
                    @NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location
            ) {
                throw new AssertionError(MessageRenderer.PLAIN.render(severity, message, location));
            }
        });

        StringBuilder sb = new StringBuilder();
        Printer p = new Printer(sb);
        for (ModuleDescription description : result) {
            p.println(description);
        }

        String actual = sb.toString();

        if (!txtFile.exists()) {
            FileUtil.writeToFile(txtFile, actual);
            fail("Expected data file does not exist. A new file created: " + txtFile);
        }

        assertEquals(FileUtil.loadFile(txtFile, true), actual);
    }
}
