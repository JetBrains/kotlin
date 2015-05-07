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

public class OneElementFMap<V> implements KeyFMap {
  private final Key myKey;
  private final V myValue;

  public OneElementFMap(@NotNull Key key, @NotNull V value) {
    myKey = key;
    myValue = value;
  }

  @NotNull
  @Override
  public <V> KeyFMap plus(@NotNull Key<V> key, @NotNull V value) {
    if (myKey == key) return new OneElementFMap<V>(key, value);
    return new PairElementsFMap(myKey, myValue, key, value);
  }

  @NotNull
  @Override
  public KeyFMap minus(@NotNull Key<?> key) {
    return key == myKey ? KeyFMap.EMPTY_MAP : this;
  }

  @Override
  public <V> V get(@NotNull Key<V> key) {
    //noinspection unchecked
    return myKey == key ? (V)myValue : null;
  }

  @NotNull
  @Override
  public Key[] getKeys() {
    return new Key[] { myKey };
  }

  @Override
  public String toString() {
    return "<" + myKey + " -> " + myValue+">";
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  public Key getKey() {
    return myKey;
  }

  public V getValue() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OneElementFMap)) return false;

    OneElementFMap map = (OneElementFMap)o;

    if (myKey != map.myKey) return false;
    if (!myValue.equals(map.myValue)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myKey.hashCode();
    result = 31 * result + myValue.hashCode();
    return result;
  }
}
