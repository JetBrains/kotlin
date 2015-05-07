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
package org.jetbrains.kotlin.util.userDataHolder.keyFMap;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

class PairElementsFMap implements KeyFMap {
  private final Key key1;
  private final Key key2;
  private final Object value1;
  private final Object value2;

  PairElementsFMap(@NotNull Key key1, @NotNull Object value1, @NotNull Key key2, @NotNull Object value2) {
    this.key1 = key1;
    this.value1 = value1;
    this.key2 = key2;
    this.value2 = value2;
    assert key1 != key2;
  }

  @NotNull
  @Override
  public <V> KeyFMap plus(@NotNull Key<V> key, @NotNull V value) {
    if (key == key1) return new PairElementsFMap(key, value, key2, value2);
    if (key == key2) return new PairElementsFMap(key, value, key1, value1);
    return new ArrayBackedFMap(new int[]{key1.hashCode(), key2.hashCode(), key.hashCode()}, new Object[]{value1, value2, value});
  }

  @NotNull
  @Override
  public KeyFMap minus(@NotNull Key<?> key) {
    if (key == key1) return new OneElementFMap<Object>(key2, value2);
    if (key == key2) return new OneElementFMap<Object>(key1, value1);
    return this;
  }

  @Override
  public <V> V get(@NotNull Key<V> key) {
    //noinspection unchecked
    return key == key1 ? (V)value1 : key == key2 ? (V)value2 : null;
  }

  @NotNull
  @Override
  public Key[] getKeys() {
    return new Key[] { key1, key2 };
  }

  @Override
  public String toString() {
    return "Pair: (" + key1 + " -> " + value1 + "; " + key2 + " -> " + value2 + ")";
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
