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

package org.jetbrains.jet.buildtools.core;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public final class Util {
    private Util() {
    }

    /**
     * {@code file.getCanonicalPath()} convenience wrapper.
     *
     * @param f file to get its canonical path.
     * @return file's canonical path
     */
    @NotNull
    public static String getPath(@NotNull File f) {
        try {
            return f.getCanonicalPath();
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("Failed to resolve canonical file of [%s]: %s", f, e), e);
        }
    }

    @NotNull
    public static String[] getPaths(String[] paths) {
        String[] result = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            result[i] = getPath(new File(path));
        }
        return result;
    }
}
