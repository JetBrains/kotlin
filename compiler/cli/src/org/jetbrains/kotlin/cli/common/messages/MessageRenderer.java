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

package org.jetbrains.kotlin.cli.common.messages;

import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.fileUtils.FileUtilsKt;

import java.io.File;

public interface MessageRenderer {

    MessageRenderer XML = new XmlMessageRenderer();

    MessageRenderer WITHOUT_PATHS = new PlainTextMessageRenderer() {
        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return null;
        }
    };

    MessageRenderer PLAIN_FULL_PATHS = new PlainTextMessageRenderer() {
        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return location.getPath();
        }
    };

    MessageRenderer PLAIN_RELATIVE_PATHS = new PlainTextMessageRenderer() {
        @NotNull
        private final File cwd = new File(".").getAbsoluteFile();

        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            String path = location.getPath();
            return path == null ? path : FileUtilsKt.descendantRelativeTo(new File(path), cwd).getPath();
        }
    };

    String renderPreamble();

    String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location);

    String renderConclusion();
}
