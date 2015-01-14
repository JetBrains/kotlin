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

package org.jetbrains.kotlin.jvm.compiler.longTest;

import org.jetbrains.annotations.NotNull;

public class LibFromMaven {
    @NotNull
    private final String org;
    @NotNull
    private final String module;
    @NotNull
    private final String rev;

    public LibFromMaven(@NotNull String org, @NotNull String module, @NotNull String rev) {
        this.org = org;
        this.module = module;
        this.rev = rev;
    }

    @NotNull
    public String getOrg() {
        return org;
    }

    @NotNull
    public String getModule() {
        return module;
    }

    @NotNull
    public String getRev() {
        return rev;
    }

    @Override
    public String toString() {
        return org + "/" + module + "/" + rev;
    }
}
