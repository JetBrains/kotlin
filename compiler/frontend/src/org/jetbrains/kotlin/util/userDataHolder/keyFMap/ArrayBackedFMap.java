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

public class ArrayBackedFMap implements KeyFMap {
  static final int ARRAY_THRESHOLD = 8;
  private final int[] keys;
  private final Object[] values;

  ArrayBackedFMap(@NotNull int[] keys, @NotNull Object[] values) {
    this.keys = keys;
    this.values = values;
  }

  @NotNull
  @Override
  public <V> KeyFMap plus(@NotNull Key<V> key, @NotNull V value) {
    int oldSize = size();
    int keyCode = key.hashCode();
    int[] newKeys = null;
    Object[] newValues = null;
    int i;
    for (i = 0; i < oldSize; i++) {
      int oldKey = keys[i];
      if (keyCode == oldKey) {
        if (value == values[i]) return this;
        newKeys = new int[oldSize];
        newValues = new Object[oldSize];
        System.arraycopy(keys, 0, newKeys, 0, oldSize);
        System.arraycopy(values, 0, newValues, 0, oldSize);
        newValues[i] = value;
        break;
      }
    }
    if (i == oldSize) {
      if (oldSize == ARRAY_THRESHOLD) {
        return new MapBackedFMap(keys, keyCode, values, value);
      }
      int newSize = oldSize + 1;
      newKeys = new int[newSize];
      newValues = new Object[newSize];
      System.arraycopy(keys, 0, newKeys, 0, oldSize);
      System.arraycopy(values, 0, newValues, 0, oldSize);
      newKeys[oldSize] = keyCode;
      newValues[oldSize] = value;
    }
    return new ArrayBackedFMap(newKeys, newValues);
  }

  private int size() {
    return keys.length;
  }

  @NotNull
  @Override
  public KeyFMap minus(@NotNull Key<?> key) {
    int oldSize = size();
    int keyCode = key.hashCode();
    for (int i = 0; i< oldSize; i++) {
      int oldKey = keys[i];
      if (keyCode == oldKey) {
        if (oldSize == 3) {
          int i1 = (2-i)/2;
          int i2 = 3 - (i+2)/2;
          Key<Object> key1 = Key.getKeyByIndex(keys[i1]);
          Key<Object> key2 = Key.getKeyByIndex(keys[i2]);
          if (key1 == null && key2 == null) return EMPTY_MAP;
          if (key1 == null) return new OneElementFMap<Object>(key2, values[i2]);
          if (key2 == null) return new OneElementFMap<Object>(key1, values[i1]);
          return new PairElementsFMap(key1, values[i1], key2, values[i2]);
        }
        int newSize = oldSize - 1;
        int[] newKeys = new int[newSize];
        Object[] newValues = new Object[newSize];
        System.arraycopy(keys, 0, newKeys, 0, i);
        System.arraycopy(values, 0, newValues, 0, i);
        System.arraycopy(keys, i+1, newKeys, i, oldSize-i-1);
        System.arraycopy(values, i+1, newValues, i, oldSize-i-1);
        return new ArrayBackedFMap(newKeys, newValues);
      }
    }
    return this;
    //if (i == oldSize) {
      //newKeys = new int[oldSize];
      //newValues = new Object[oldSize];
      //System.arraycopy(keys, 0, newKeys, 0, oldSize);
      //System.arraycopy(values, 0, newValues, 0, oldSize);
    //}

  }

  @Override
  public <V> V get(@NotNull Key<V> key) {
    int oldSize = size();
    int keyCode = key.hashCode();
    for (int i = 0; i < oldSize; i++) {
      int oldKey = keys[i];
      if (keyCode == oldKey) {
        //noinspection unchecked
        return (V)values[i];
      }
    }
    return null;
  }

  @Override
  public String toString() {
    String s = "";
    for (int i = 0; i < keys.length; i++) {
      int key = keys[i];
      Object value = values[i];
      s += (s.isEmpty() ? "" : ", ") + Key.getKeyByIndex(key) + " -> " + value;
    }
    return "(" + s + ")";
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @NotNull
  public int[] getKeyIds() {
    return keys;
  }

  @NotNull
  @Override
  public Key[] getKeys() {
    return getKeysByIndices(keys);
  }

  @NotNull
  public Object[] getValues() {
    return values;
  }

  @NotNull
  static Key[] getKeysByIndices(int[] indexes) {
    Key[] result = new Key[indexes.length];

    for (int i =0; i < indexes.length; i++) {
      result[i] = Key.getKeyByIndex(indexes[i]);
    }

    return result;
  }
}
