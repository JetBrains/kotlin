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

package org.jetbrains.jet.compiler.runner;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractOutputItemCollector<S, R> implements OutputItemsCollector {
    private static final String FOR_SOURCE_PREFIX = "For source: ";
    private static final String EMITTING_PREFIX = "Emitting: ";
    private S currentSource;
    private final List<R> answer = ContainerUtil.newArrayList();
    private final List<S> sources = ContainerUtil.newArrayList();

    private final String outputPath;

    public AbstractOutputItemCollector(@NotNull String outputPath) {
        this.outputPath = outputPath;
    }

    protected void addItem(R item) {
        answer.add(item);
    }

    @Override
    public final void learn(String message) {
        message = message.trim();
        if (message.startsWith(FOR_SOURCE_PREFIX)) {
            String sourcePath = message.substring(FOR_SOURCE_PREFIX.length());
            currentSource = convertSource(sourcePath);
            if (currentSource != null) {
                sources.add(currentSource);
            }
        }
        else if (message.startsWith(EMITTING_PREFIX)) {
            if (currentSource != null) {
                String resultPath = message.substring(EMITTING_PREFIX.length());
                R item = convertResult(outputPath + "/" + resultPath, currentSource);
                if (item != null) {
                    answer.add(item);
                }
            }
        }
    }

    protected abstract R convertResult(String resultPath, S correspondingSource);

    protected abstract S convertSource(String sourcePath);

    public List<R> getOutputs() {
        return answer;
    }

    public List<S> getSources() {
        return sources;
    }
}
