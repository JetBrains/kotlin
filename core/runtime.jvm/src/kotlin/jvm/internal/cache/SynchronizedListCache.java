/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.jvm.internal.cache;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class SynchronizedListCache<Key, Value> implements Cache<Key, Value> {
    private final LinkedList<Pair<Key, Value>> buffer = new LinkedList<Pair<Key, Value>>();

    @Nullable
    @Override
    public synchronized Value get(Key key) {
        for (Pair<Key, Value> item : buffer) {
            if (item.key.equals(key)) {
                return item.value;
            }
        }

        return null;
    }

    @Override
    public synchronized void set(Key key, Value value) {
        buffer.add(new Pair<Key, Value>(key, value));
    }

    private static class Pair<Key, Value> {
        public final Key key;
        public final Value value;

        private Pair(Key key, Value value) {
            this.key = key;
            this.value = value;
        }
    }
}
