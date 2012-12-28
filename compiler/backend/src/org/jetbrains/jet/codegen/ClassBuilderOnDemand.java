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

package org.jetbrains.jet.codegen;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ClassBuilderOnDemand {

    private ClassBuilder classBuilder;

    private final List<ClassBuilderCallback> optionalDeclarations = Lists.newArrayList();

    interface ClassBuilderCallback {
        void doSomething(@NotNull ClassBuilder classBuilder);
    }

    @NotNull
    protected abstract ClassBuilder createClassBuilder();

    public void addOptionalDeclaration(@NotNull ClassBuilderCallback callback) {
        optionalDeclarations.add(callback);
        if (classBuilder != null) {
            callback.doSomething(classBuilder);
        }
    }

    @NotNull
    public ClassBuilder getClassBuilder() {
        if (classBuilder == null) {
            classBuilder = createClassBuilder();
            for (ClassBuilderCallback callback : optionalDeclarations) {
                callback.doSomething(classBuilder);
            }
        }
        return classBuilder;
    }

    public void done() {
        if (classBuilder != null) {
            classBuilder.done();
        }
    }

    public boolean isActivated() {
        return classBuilder != null;
    }
}
