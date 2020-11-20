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

package org.jetbrains.kotlin.idea.core;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.kotlin.idea.KotlinFileType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KotlinFileTypeFactoryUtils {
    public final static String[] KOTLIN_EXTENSIONS = new String[] { "kt", "kts" };
    private final static FileType[] KOTLIN_FILE_TYPES = new FileType[] { KotlinFileType.INSTANCE };
    public final static Set<FileType> KOTLIN_FILE_TYPES_SET = new HashSet<>(Arrays.asList(KOTLIN_FILE_TYPES));
}
