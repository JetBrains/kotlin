/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.android.tools.r8.*;
import com.android.tools.r8.origin.PathOrigin;
import kotlin.Pair;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.junit.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class D8Checker {

    private D8Checker() {
    }

    public static void check(ClassFileFactory outputFiles) {
        runD8(builder -> {
            for (OutputFile file : ClassFileUtilsKt.getClassFiles(outputFiles)) {
                byte[] bytes = file.asByteArray();
                builder.addClassProgramData(bytes, new PathOrigin(Paths.get(file.getRelativePath())));
            }
        });
    }

    public static void checkFilesWithD8(Collection<Pair<byte[], String>> classFiles) {
        runD8(builder -> {
            classFiles.forEach(pair -> {
                builder.addClassProgramData(pair.getFirst(), new PathOrigin(Paths.get(pair.getSecond())));
            });
        });
    }

    // Compilation with D8 should proceed with no output. There should be no info, warnings, or errors.
    static class TestDiagnosticsHandler implements DiagnosticsHandler {
        @Override
        public void error(Diagnostic diagnostic) {
            Assert.fail("D8 dexing error: " + diagnostic.getDiagnosticMessage());
        }

        @Override
        public void warning(Diagnostic diagnostic) {
            Assert.fail("D8 dexing warning: " + diagnostic.getDiagnosticMessage());
        }

        @Override
        public void info(Diagnostic diagnostic) {
            Assert.fail("D8 dexing info: " + diagnostic.getDiagnosticMessage());
        }
    }

    private static void runD8(Consumer<D8Command.Builder> addInput) {
        ProgramConsumer ignoreOutputConsumer = new DexIndexedConsumer.ForwardingConsumer(null);
        D8Command.Builder builder = D8Command.builder(new TestDiagnosticsHandler())
                .setMinApiLevel(28)
                .setMode(CompilationMode.DEBUG)
                .setProgramConsumer(ignoreOutputConsumer);
        addInput.accept(builder);
        try {
            D8.run(builder.build(), Executors.newSingleThreadExecutor());
        }
        catch (CompilationFailedException e) {
            Assert.fail(generateExceptionMessage(e));
        }
    }

    private static String generateExceptionMessage(Throwable e) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            e.printStackTrace(printWriter);
            String stackTrace = writer.toString();
            return stackTrace;
        }
    }
}
