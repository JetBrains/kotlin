/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.web;

import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SourceMapsUtils {
    @NotNull
    public static String calculateSourceMapSourceRoot(
            @NotNull MessageCollector messageCollector,
            @NotNull List<String> freeArguments
    ) {
        File commonPath = null;
        List<File> pathToRoot = new ArrayList<>();
        Map<File, Integer> pathToRootIndexes = new HashMap<>();

        try {
            for (String path : freeArguments) {
                File file = new File(path).getCanonicalFile();
                if (commonPath == null) {
                    commonPath = file;

                    while (file != null) {
                        pathToRoot.add(file);
                        file = file.getParentFile();
                    }
                    Collections.reverse(pathToRoot);

                    for (int i = 0; i < pathToRoot.size(); ++i) {
                        pathToRootIndexes.put(pathToRoot.get(i), i);
                    }
                }
                else {
                    while (file != null) {
                        Integer existingIndex = pathToRootIndexes.get(file);
                        if (existingIndex != null) {
                            existingIndex = Math.min(existingIndex, pathToRoot.size() - 1);
                            pathToRoot.subList(existingIndex + 1, pathToRoot.size()).clear();
                            commonPath = pathToRoot.get(pathToRoot.size() - 1);
                            break;
                        }
                        file = file.getParentFile();
                    }
                    if (file == null) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            String text = ExceptionUtil.getThrowableText(e);
            messageCollector.report(CompilerMessageSeverity.ERROR, "IO error occurred calculating source root:\n" + text, null);
            return ".";
        }

        return commonPath != null ? commonPath.getPath() : ".";
    }
}
