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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.fileUtils.FileUtilsKt;

import java.io.File;

public interface MessageRenderer {

    String PROPERTY_KEY = "org.jetbrains.kotlin.cliMessageRenderer";

    MessageRenderer XML = new XmlMessageRenderer();

    MessageRenderer WITHOUT_PATHS = new PlainTextMessageRenderer() {
        @Nullable
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return null;
        }

        @Override
        public String getName() {
            return "Pathless";
        }
    };

    MessageRenderer PLAIN_FULL_PATHS = new PlainTextMessageRenderer() {
        @NotNull
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return location.getPath();
        }

        @Override
        public String getName() {
            return "FullPath";
        }
    };

    MessageRenderer PLAIN_RELATIVE_PATHS = new PlainTextMessageRenderer() {
        private final File cwd = new File(".").getAbsoluteFile();

        @NotNull
        @Override
        protected String getPath(@NotNull CompilerMessageLocation location) {
            return FileUtilsKt.descendantRelativeTo(new File(location.getPath()), cwd).getPath();
        }

        @Override
        public String getName() {
            return "RelativePath";
        }
    };

    MessageRenderer GRADLE_STYLE = new GradleStyleMessageRenderer();

    String renderPreamble();

    String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageLocation location);

    String renderUsage(@NotNull String usage);

    String renderConclusion();

    String getName();
}
