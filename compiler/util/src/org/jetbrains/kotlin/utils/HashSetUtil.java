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

package org.jetbrains.kotlin.utils;

import com.intellij.util.containers.hash.EqualityPolicy;
import com.intellij.util.containers.hash.LinkedHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class HashSetUtil {
    @NotNull
    public static <T> Set<T> linkedHashSet(@NotNull Set<T> set,  @NotNull EqualityPolicy<T> policy) {
        // this implementation of LinkedHashMap doesn't admit nulls as values
        Map<T, String> map = new LinkedHashMap<T, String>(policy);
        for (T t : set) {
            map.put(t, "");
        }
        return map.keySet();
    }
}
