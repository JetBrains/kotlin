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

package org.jetbrains.kotlin.resolve;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetScript;

// SCRIPT: Resolve declarations in scripts
public class ScriptPriorities {

    public static final Key<Integer> PRIORITY_KEY = Key.create(JetScript.class.getName() + ".priority");

    public static int getScriptPriority(@NotNull JetScript script) {
        Integer priority = script.getUserData(PRIORITY_KEY);
        return priority == null ? 0 : priority;
    }
}
