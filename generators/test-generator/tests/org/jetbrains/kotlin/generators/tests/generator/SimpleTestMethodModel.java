/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.isIgnoredTarget;

public class SimpleTestMethodModel extends TestMethodModel {

    @NotNull
    private final File rootDir;
    @NotNull
    protected final File file;
    @NotNull
    private final Pattern filenamePattern;
    @NotNull
    protected final TargetBackend targetBackend;

    private final boolean skipIgnored;

    public SimpleTestMethodModel(
            @NotNull File rootDir,
            @NotNull File file,
            @NotNull Pattern filenamePattern,
            @Nullable Boolean checkFilenameStartsLowerCase,
            @NotNull TargetBackend targetBackend,
            boolean skipIgnored
    ) {
        this.rootDir = rootDir;
        this.file = file;
        this.filenamePattern = filenamePattern;
        this.targetBackend = targetBackend;
        this.skipIgnored = skipIgnored;

        if (checkFilenameStartsLowerCase != null) {
            char c = file.getName().charAt(0);
            if (checkFilenameStartsLowerCase) {
                assert Character.isLowerCase(c) : "Invalid file name '" + file + "', file name should start with lower-case letter";
            }
            else {
                assert Character.isUpperCase(c) : "Invalid file name '" + file + "', file name should start with upper-case letter";
            }
        }
    }

    @Override
    public void generateBody(@NotNull Printer p) {
        String filePath = KotlinTestUtils.getFilePath(file) + (file.isDirectory() ? "/" : "");
        p.println(RunTestMethodModel.METHOD_NAME, "(\"", filePath, "\");");
    }

    @Override
    public String getDataString() {
        String path = FileUtil.getRelativePath(rootDir, file);
        assert path != null;
        return KotlinTestUtils.getFilePath(new File(path));
    }

    @Override
    public boolean shouldBeGenerated() {
        return InTextDirectivesUtils.isCompatibleTarget(targetBackend, file);
    }

    @NotNull
    @Override
    public String getName() {
        Matcher matcher = filenamePattern.matcher(file.getName());
        boolean found = matcher.find();
        assert found : file.getName() + " isn't matched by regex " + filenamePattern.pattern();
        assert matcher.groupCount() >= 1 : filenamePattern.pattern();
        String extractedName = matcher.group(1);
        assert extractedName != null : "extractedName should not be null: "  + filenamePattern.pattern();

        String unescapedName;
        if (rootDir.equals(file.getParentFile())) {
            unescapedName = extractedName;
        }
        else {
            String relativePath = FileUtil.getRelativePath(rootDir, file.getParentFile());
            unescapedName = relativePath + "-" + StringsKt.capitalize(extractedName);
        }

        boolean ignored = skipIgnored && isIgnoredTarget(targetBackend, file);
        return (ignored ? "ignore" : "test") + StringsKt.capitalize(TestGeneratorUtil.escapeForJavaIdentifier(unescapedName));
    }
}
