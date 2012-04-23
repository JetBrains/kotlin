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

package org.jetbrains.jet.plugin.compiler;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.compiler.CompilerMessageCategory.*;

/**
* @author abreslav
*/
public class CompilerProcessListener extends ProcessAdapter {
    private static final Logger LOG = Logger.getInstance("#org.jetbrains.jet.plugin.compiler.CompilerProcessListener");

    private static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile("<(ERROR|WARNING|INFO|EXCEPTION|LOGGING)", Pattern.MULTILINE);
    private static final Pattern OPEN_TAG_END_PATTERN = Pattern.compile(">", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\s*(path|line|column)\\s*=\\s*\"(.*?)\"", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(.*?)</(ERROR|WARNING|INFO|EXCEPTION|LOGGING)>", Pattern.MULTILINE | Pattern.DOTALL);

    private enum State {
        WAITING, ATTRIBUTES, MESSAGE
    }

    private static class CompilerMessage {
        private CompilerMessageCategory messageCategory;
        private boolean isException;
        private @Nullable String url;
        private @Nullable Integer line;
        private @Nullable Integer column;
        private String message;

        public void setMessageCategoryFromString(String tagName) {
            if ("ERROR".equals(tagName)) {
                messageCategory = ERROR;
            }
            else if ("EXCEPTION".equals(tagName)) {
                messageCategory = ERROR;
                isException = true;
            }
            else if ("WARNING".equals(tagName)) {
                messageCategory = WARNING;
            }
            else if ("LOGGING".equals(tagName)) {
                messageCategory = STATISTICS;
            }
            else {
                messageCategory = INFORMATION;
            }
        }

        public void setAttributeFromStrings(String name, String value) {
            if ("path".equals(name)) {
                url = "file://" + value.trim();
            }
            else if ("line".equals(name)) {
                line = safeParseInt(value);
            }
            else if ("column".equals(name)) {
                column = safeParseInt(value);
            }
        }

        @Nullable
        private static Integer safeParseInt(String value) {
            try {
                return Integer.parseInt(value.trim());
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void reportTo(CompileContext compileContext) {
            if (messageCategory == STATISTICS) {
                compileContext.getProgressIndicator().setText(message);
            }
            else {
                compileContext.addMessage(messageCategory, message, url == null ? "" : url, line == null ? -1 : line,
                                          column == null
                                          ? -1
                                          : column);
                if (isException) {
                    LOG.error(message);
                }
            }
        }
    }

    private final CompileContext compileContext;
    private final OutputItemsCollector collector;
    private final StringBuilder output = new StringBuilder();
    private int firstUnprocessedIndex = 0;
    private State state = State.WAITING;
    private CompilerMessage currentCompilerMessage;

    public CompilerProcessListener(CompileContext compileContext, OutputItemsCollector collector) {
        this.compileContext = compileContext;
        this.collector = collector;
    }

    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
        String text = event.getText();
        if (outputType == ProcessOutputTypes.STDERR) {
            output.append(text);

            // We loop until the state stabilizes
            State lastState;
            do {
                lastState = state;
                switch (state) {
                    case WAITING: {
                        Matcher matcher = matcher(DIAGNOSTIC_PATTERN);
                        if (find(matcher)) {
                            currentCompilerMessage = new CompilerMessage();
                            currentCompilerMessage.setMessageCategoryFromString(matcher.group(1));
                            state = State.ATTRIBUTES;
                        }
                        break;
                    }
                    case ATTRIBUTES: {
                        Matcher matcher = matcher(ATTRIBUTE_PATTERN);
                        int indexDelta = 0;
                        while (matcher.find()) {
                            handleSkippedOutput(output.subSequence(firstUnprocessedIndex + indexDelta, firstUnprocessedIndex + matcher.start()));
                            currentCompilerMessage.setAttributeFromStrings(matcher.group(1), matcher.group(2));
                            indexDelta = matcher.end();

                        }
                        firstUnprocessedIndex += indexDelta;

                        Matcher endMatcher = matcher(OPEN_TAG_END_PATTERN);
                        if (find(endMatcher)) {
                            state = State.MESSAGE;
                        }
                        break;
                    }
                    case MESSAGE: {
                        Matcher matcher = matcher(MESSAGE_PATTERN);
                        if (find(matcher)) {
                            String message = matcher.group(1);
                            currentCompilerMessage.setMessage(message);
                            currentCompilerMessage.reportTo(compileContext);

                            if (currentCompilerMessage.messageCategory == STATISTICS) {
                                collector.learn(message);
                            }

                            state = State.WAITING;
                        }
                        break;
                    }
                }
            }
            while (state != lastState);

        }
        else {
            compileContext.addMessage(INFORMATION, text, "", -1, -1);
        }
    }

    private boolean find(Matcher matcher) {
        boolean result = matcher.find();
        if (result) {
            handleSkippedOutput(output.subSequence(firstUnprocessedIndex, firstUnprocessedIndex + matcher.start()));
            firstUnprocessedIndex += matcher.end();
        }
        return result;
    }

    private Matcher matcher(Pattern pattern) {
        return pattern.matcher(output.subSequence(firstUnprocessedIndex, output.length()));
    }

    @Override
    public void processTerminated(ProcessEvent event) {
        if (firstUnprocessedIndex < output.length()) {
            handleSkippedOutput(output.substring(firstUnprocessedIndex).trim());
        }
        int exitCode = event.getExitCode();
        // 0 is normal, 1 is "errors found" - handled by the messages above
        if (exitCode != 0 && exitCode != 1) {
            compileContext.addMessage(ERROR, "Compiler terminated with exit code: " + exitCode, "", -1, -1);
        }
    }

    private void handleSkippedOutput(CharSequence substring) {
        String message = substring.toString();
        if (!message.trim().isEmpty()) {
            compileContext.addMessage(ERROR, message, "", -1, -1);
        }
    }
}
