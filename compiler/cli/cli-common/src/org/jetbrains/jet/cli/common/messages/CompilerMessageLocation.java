/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.cli.common.messages;

import org.jetbrains.annotations.Nullable;

public class CompilerMessageLocation {

    public static final CompilerMessageLocation NO_LOCATION = new CompilerMessageLocation(null, -1, -1);

    public static CompilerMessageLocation create(@Nullable String path, int line, int column) {
        if (path == null) {
            return NO_LOCATION;
        }
        return new CompilerMessageLocation(path, line, column);
    }

    private final String path;
    private final int line;
    private final int column;

    private CompilerMessageLocation(@Nullable String path, int line, int column) {
        this.path = path;
        this.line = line;
        this.column = column;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompilerMessageLocation location = (CompilerMessageLocation) o;

        if (column != location.column) return false;
        if (line != location.line) return false;
        if (path != null ? !path.equals(location.path) : location.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + line;
        result = 31 * result + column;
        return result;
    }
}
