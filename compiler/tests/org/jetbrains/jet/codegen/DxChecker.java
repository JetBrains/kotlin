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

import com.android.dx.command.dexer.Main;
import com.android.dx.dex.cf.CfTranslator;
import junit.framework.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DxChecker {

    public static final boolean RUN_DX_CHECKER = true;
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("[\\s]+at .*");

    private DxChecker() {
    }

    public static void check(ClassFileFactory factory) {
        Main.Arguments arguments = new Main.Arguments();
        String[] array = new String[1];
        array[0] = "testArgs";
        arguments.parse(array);

        for (String file : factory.files()) {
            try {
                CfTranslator.translate(file, factory.asBytes(file), arguments.cfOptions, arguments.dexOptions);
            }
            catch (Throwable e) {
                Assert.fail(generateExceptionMessage(e));
            }
        }
    }

    private static String generateExceptionMessage(Throwable e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        try {
            e.printStackTrace(printWriter);
            String stackTrace = writer.toString();
            Matcher matcher = STACK_TRACE_PATTERN.matcher(stackTrace);
            return matcher.replaceAll("");
        }
        finally {
            printWriter.close();
        }
    }
}
