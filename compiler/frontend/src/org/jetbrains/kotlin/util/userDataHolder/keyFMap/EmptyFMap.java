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

class EmptyFMap implements KeyFMap {
  private static final Key[] EMPTY_KEYS_ARRAY = {};

  EmptyFMap() {
  }

  @NotNull
  @Override
  public <V> KeyFMap plus(@NotNull Key<V> key, @NotNull V value) {
    return new OneElementFMap<V>(key, value);
  }

  @NotNull
  @Override
  public KeyFMap minus(@NotNull Key<?> key) {
    return this;
  }

  @Override
  public <V> V get(@NotNull Key<V> key) {
    return null;
  }

  @NotNull
  @Override
  public Key[] getKeys() {
    return EMPTY_KEYS_ARRAY;
  }

  @Override
  public String toString() {
    return "<empty>";
  }

  @Override
  public boolean isEmpty() {
    return true;
  }
}
