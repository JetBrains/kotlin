/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;

public interface LabelOwner {

    boolean isMyLabel(@NotNull String name);

    LabelOwner SKIP_ALL = new LabelOwner() {
        @Override
        public boolean isMyLabel(@NotNull String name) {
            return false;
        }
    };


    LabelOwner NOT_APPLICABLE = new LabelOwner() {
        @Override
        public boolean isMyLabel(@NotNull String name) {
            throw new RuntimeException("This operation not applicable for current context");
        }
    };

}
