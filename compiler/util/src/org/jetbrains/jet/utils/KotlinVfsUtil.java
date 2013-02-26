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

package org.jetbrains.jet.utils;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class KotlinVfsUtil {
    private static final String FILE = "file";
    private static final String JAR = "jar";
    private static final String PROTOCOL_DELIMITER = ":";

    /*
     * This code is essentially copied from IntelliJ IDEA's VfsUtil, and slightly restructured
     */
    @NotNull
    public static String convertFromUrl(@NotNull URL url) throws MalformedURLException {
        String protocol = url.getProtocol();
        String path = url.getPath();
        if (JAR.equals(protocol)) {
            if (StringUtil.startsWithConcatenationOf(path, FILE, PROTOCOL_DELIMITER)) {
                URL subURL = new URL(path);
                path = subURL.getPath();
            }
            else {
                throw new MalformedURLException("Can't parse url: " + url.toExternalForm());
            }
        }
        if (SystemInfo.isWindows || SystemInfo.isOS2) {
            while (path.length() > 0 && path.charAt(0) == '/') {
                path = path.substring(1, path.length());
            }
        }

        path = URLUtil.unescapePercentSequences(path);
        return protocol + "://" + path;
    }

    public static List<File> getFilesInDirectoryByPattern(@NotNull File dir, @NotNull final Pattern pattern) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(@NotNull File dir, @NotNull String name) {
                return pattern.matcher(name).matches();
            }
        });
        return files != null ? Arrays.asList(files) : Collections.<File>emptyList();
    }


    private KotlinVfsUtil() {}
}
