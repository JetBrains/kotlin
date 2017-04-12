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

package org.jetbrains.kotlin.codegen;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.command.dexer.Main;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.DexFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.junit.Assert;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DxChecker {

    public static final boolean RUN_DX_CHECKER = true;
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("[\\s]+at .*");

    private DxChecker() {
    }

    public static void check(ClassFileFactory outputFiles) {
        Main.Arguments arguments = new Main.Arguments();
        String[] array = new String[1];
        array[0] = "testArgs";
        arguments.parse(array);

        for (OutputFile file : ClassFileUtilsKt.getClassFiles(outputFiles)) {
            try {
                byte[] bytes = file.asByteArray();
                if (isJava6Class(bytes)) {
                    checkFileWithDx(bytes, file.getRelativePath(), arguments);
                }
            }
            catch (Throwable e) {
                Assert.fail(generateExceptionMessage(e));
            }
        }
    }

    private static boolean isJava6Class(byte[] bytes) throws IOException {
        try (DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int header = stream.readInt();
            if (0xCAFEBABE != header) {
                throw new IOException("Invalid header class header: " + header);
            }
            int minor = stream.readUnsignedShort();
            int major = stream.readUnsignedShort();
            return major == Opcodes.V1_6;
        }
    }

    public static void checkFileWithDx(byte[] bytes, @NotNull String relativePath) {
        Main.Arguments arguments = new Main.Arguments();
        String[] array = new String[1];
        array[0] = "testArgs";
        arguments.parse(array);
        checkFileWithDx(bytes, relativePath, arguments);
    }

    private static void checkFileWithDx(byte[] bytes, @NotNull String relativePath, @NotNull Main.Arguments arguments) {
        DirectClassFile cf = new DirectClassFile(bytes, relativePath, true);
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        CfTranslator.translate(
                cf,
                bytes,
                arguments.cfOptions,
                arguments.dexOptions,
                new DexFile(arguments.dexOptions)
        );
    }

    private static String generateExceptionMessage(Throwable e) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            e.printStackTrace(printWriter);
            String stackTrace = writer.toString();
            Matcher matcher = STACK_TRACE_PATTERN.matcher(stackTrace);
            return matcher.replaceAll("");
        }
    }
}
