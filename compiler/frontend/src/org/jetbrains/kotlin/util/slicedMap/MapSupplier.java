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

package org.jetbrains.kotlin.util.slicedMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public interface MapSupplier {

    MapSupplier HASH_MAP_SUPPLIER = new MapSupplier() {
        @Override
        public <K, V> Map<K, V> get() {
            return new HashMap<>();
        }
    };

    MapSupplier LINKED_HASH_MAP_SUPPLIER = new MapSupplier() {
        @Override
        public <K, V> Map<K, V> get() {
            return new LinkedHashMap<>();
        }
    };

    <K, V> Map<K, V> get();
}
