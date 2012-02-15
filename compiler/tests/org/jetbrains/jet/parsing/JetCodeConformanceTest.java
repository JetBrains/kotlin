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

package org.jetbrains.jet.parsing;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class JetCodeConformanceTest extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        TestSuite ats = new TestSuite("Side-effect-free at()'s in assertions");
        suite.addTest(ats);
        File parsingSourceDir = new File("./compiler/frontend/src/org/jetbrains/jet/lang/parsing");
        for (File sourceFile : parsingSourceDir.listFiles()) {
            if (sourceFile.getName().endsWith(".java")) {
                ats.addTest(new JetCodeConformanceTest(sourceFile.getName(), sourceFile));
            }
        }
        return suite;
    }

    private final File sourceFile;

    public JetCodeConformanceTest(String name, File sourceFile) {
        super(name);
        this.sourceFile = sourceFile;
    }

    @Override
    protected void runTest() throws Throwable {
        checkSourceFile(sourceFile);
    }

    private void checkSourceFile(File sourceFile) throws IOException {
        FileReader reader = new FileReader(sourceFile);
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = reader.read()) >= 0) {
            builder.append((char) c);
        }
        String source = builder.toString();

        Pattern atPattern = Pattern.compile("assert.*?[^_]at.*?$", Pattern.MULTILINE);
        Matcher matcher = atPattern.matcher(source);
        boolean match = matcher.find();
        if (match) {
            fail("An at-method with side-ffects is used inside assert: " + matcher.group());
        }
    }


}
