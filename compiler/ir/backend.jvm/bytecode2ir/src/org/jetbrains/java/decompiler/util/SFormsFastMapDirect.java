// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent;
import org.jetbrains.java.decompiler.util.FastSparseSetFactory.FastSparseSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

// Three part map to hold live var version data for specific variables.
// Segmented to handle the cases of true variable (index 0), stack variable (index 1), and field access (index 2) seperately as they can have the same index.
public class SFormsFastMapDirect {

  private int size;

  @SuppressWarnings("unchecked") private final FastSparseSet<Integer>[][] elements = new FastSparseSet[3][];

  private final int[][] next = new int[3][];

  public SFormsFastMapDirect() {
    this(true);
  }

  private SFormsFastMapDirect(boolean initialize) {
    if (initialize) {
      for (int i = 2; i >= 0; i--) {
        @SuppressWarnings("unchecked") FastSparseSet<Integer>[] empty = FastSparseSet.EMPTY_ARRAY;
        elements[i] = empty;
        next[i] = InterpreterUtil.EMPTY_INT_ARRAY;
      }
    }
  }

  public SFormsFastMapDirect(SFormsFastMapDirect map) {
    for (int i = 2; i >= 0; i--) {
      FastSparseSet<Integer>[] arr = map.elements[i];
      int[] arrnext = map.next[i];

      int length = arr.length;
      @SuppressWarnings("unchecked") FastSparseSet<Integer>[] arrnew = new FastSparseSet[length];
      int[] arrnextnew = new int[length];

      System.arraycopy(arr, 0, arrnew, 0, length);
      System.arraycopy(arrnext, 0, arrnextnew, 0, length);

      elements[i] = arrnew;
      next[i] = arrnextnew;

      size = map.size;
    }
  }

  public SFormsFastMapDirect getCopy() {

    SFormsFastMapDirect map = new SFormsFastMapDirect(false);
    map.size = size;

    FastSparseSet[][] mapelements = map.elements;
    int[][] mapnext = map.next;

    for (int i = 2; i >= 0; i--) {
      FastSparseSet<Integer>[] arr = elements[i];
      int length = arr.length;

      if (length > 0) {
        int[] arrnext = next[i];

        @SuppressWarnings("unchecked") FastSparseSet<Integer>[] arrnew = new FastSparseSet[length];
        int[] arrnextnew = Arrays.copyOf(arrnext, length);

        mapelements[i] = arrnew;
        mapnext[i] = arrnextnew;

        int pointer = 0;
        do {
          FastSparseSet<Integer> set = arr[pointer];
          if (set != null) {
            arrnew[pointer] = set.getCopy();
          }

          pointer = arrnext[pointer];
        }
        while (pointer != 0);
      } else {
        mapelements[i] = FastSparseSet.EMPTY_ARRAY;
        mapnext[i] = InterpreterUtil.EMPTY_INT_ARRAY;
      }
    }

    return map;
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void put(int key, FastSparseSet<Integer> value) {
    putInternal(key, value, false);
  }

  public void remove(int key) {
    putInternal(key, null, true);
  }

  public void removeAllFields() {
    FastSparseSet<Integer>[] arr = elements[2];
    int[] arrnext = next[2];

    for (int i = arr.length - 1; i >= 0; i--) {
      FastSparseSet<Integer> val = arr[i];
      if (val != null) {
        arr[i] = null;
        size--;
      }
      arrnext[i] = 0;
    }
  }

  public void removeAllStacks() {
    FastSparseSet<Integer>[] arr = elements[1];
    int[] arrnext = next[1];

    for (int i = arr.length - 1; i >= 0; i--) {
      FastSparseSet<Integer> val = arr[i];
      if (val != null) {
        arr[i] = null;
        size--;
      }
      arrnext[i] = 0;
    }
  }

  private void putInternal(final int key, final FastSparseSet<Integer> value, boolean remove) {
    int index = 0;
    int ikey = key;
    if (ikey < 0) {
      index = 2;
      ikey = -ikey;
    } else if (ikey >= VarExprent.STACK_BASE) {
      index = 1;
      ikey -= VarExprent.STACK_BASE;
    }

    FastSparseSet<Integer>[] arr = elements[index];
    if (ikey >= arr.length) {
      if (remove) {
        return;
      } else {
        arr = ensureCapacity(index, ikey + 1, false);
      }
    }

    FastSparseSet<Integer> oldval = arr[ikey];
    arr[ikey] = value;

    int[] arrnext = next[index];

    if (oldval == null && value != null) {
      size++;
      changeNext(arrnext, ikey, arrnext[ikey], ikey);
    } else if (oldval != null && value == null) {
      size--;
      changeNext(arrnext, ikey, ikey, arrnext[ikey]);
    }
  }

  private static void changeNext(int[] arrnext, int key, int oldnext, int newnext) {
    for (int i = key - 1; i >= 0; i--) {
      if (arrnext[i] == oldnext) {
        arrnext[i] = newnext;
      } else {
        break;
      }
    }
  }

  public boolean containsKey(int key) {
    return get(key) != null;
  }

  public FastSparseSet<Integer> get(int key) {

    int index = 0;
    if (key < 0) {
      index = 2;
      key = -key;
    } else if (key >= VarExprent.STACK_BASE) {
      index = 1;
      key -= VarExprent.STACK_BASE;
    }

    FastSparseSet<Integer>[] arr = elements[index];

    if (key < arr.length) {
      return arr[key];
    }

    return null;
  }

  public void complement(SFormsFastMapDirect map) {

    for (int i = 2; i >= 0; i--) {
      FastSparseSet<Integer>[] lstOwn = elements[i];

      if (lstOwn.length == 0) {
        continue;
      }

      FastSparseSet<Integer>[] lstExtern = map.elements[i];
      int[] arrnext = next[i];

      int pointer = 0;
      do {
        FastSparseSet<Integer> first = lstOwn[pointer];

        if (first != null) {
          if (pointer >= lstExtern.length) {
            break;
          }
          FastSparseSet<Integer> second = lstExtern[pointer];

          if (second != null) {
            first.complement(second);
            if (first.isEmpty()) {
              lstOwn[pointer] = null;
              size--;
              changeNext(arrnext, pointer, pointer, arrnext[pointer]);
            }
          }
        }

        pointer = arrnext[pointer];
      }
      while (pointer != 0);
    }
  }

  public void intersection(SFormsFastMapDirect map) {

    for (int i = 2; i >= 0; i--) {
      FastSparseSet<Integer>[] lstOwn = elements[i];

      if (lstOwn.length == 0) {
        continue;
      }

      FastSparseSet<Integer>[] lstExtern = map.elements[i];
      int[] arrnext = next[i];

      int pointer = 0;
      do {
        FastSparseSet<Integer> first = lstOwn[pointer];

        if (first != null) {
          FastSparseSet<Integer> second = null;
          if (pointer < lstExtern.length) {
            second = lstExtern[pointer];
          }

          if (second != null) {
            first.intersection(second);
          }

          if (second == null || first.isEmpty()) {
            lstOwn[pointer] = null;
            size--;
            changeNext(arrnext, pointer, pointer, arrnext[pointer]);
          }
        }

        pointer = arrnext[pointer];
      }
      while (pointer != 0);
    }
  }

  public void union(SFormsFastMapDirect map) {

    for (int i = 2; i >= 0; i--) {
      FastSparseSet<Integer>[] lstExtern = map.elements[i];

      if (lstExtern.length == 0) {
        continue;
      }

      FastSparseSet<Integer>[] lstOwn = elements[i];
      int[] arrnext = next[i];
      int[] arrnextExtern = map.next[i];

      int pointer = 0;
      do {
        if (pointer >= lstOwn.length) {
          lstOwn = ensureCapacity(i, lstExtern.length, true);
          arrnext = next[i];
        }

        FastSparseSet<Integer> second = lstExtern[pointer];

        if (second != null) {
          FastSparseSet<Integer> first = lstOwn[pointer];

          if (first == null) {
            lstOwn[pointer] = second.getCopy();
            size++;
            changeNext(arrnext, pointer, arrnext[pointer], pointer);
          }
          else {
            first.union(second);
          }
        }

        pointer = arrnextExtern[pointer];
      }
      while (pointer != 0);
    }
  }

  public String toString() {

    StringBuilder buffer = new StringBuilder("{");

    List<Entry<Integer, FastSparseSet<Integer>>> lst = entryList();
    if (lst != null) {
      boolean first = true;
      for (Entry<Integer, FastSparseSet<Integer>> entry : lst) {
        if (!first) {
          buffer.append(", ");
        }
        else {
          first = false;
        }

        Set<Integer> set = entry.getValue().toPlainSet();
        buffer.append(entry.getKey()).append("={").append(set.toString()).append("}");
      }
    }

    buffer.append("}");
    return buffer.toString();
  }

  public List<Entry<Integer, FastSparseSet<Integer>>> entryList() {
    List<Entry<Integer, FastSparseSet<Integer>>> list = new ArrayList<>();

    for (int i = 2; i >= 0; i--) {
      int ikey = 0;
      for (final FastSparseSet<Integer> ent : elements[i]) {
        if (ent != null) {
          final int key = i == 0 ? ikey : (i == 1 ? ikey + VarExprent.STACK_BASE : -ikey);

          list.add(new Entry<Integer, FastSparseSet<Integer>>() {

            @Override
            public Integer getKey() {
              return key;
            }

            @Override
            public FastSparseSet<Integer> getValue() {
              return ent;
            }

            @Override
            public FastSparseSet<Integer> setValue(FastSparseSet<Integer> newvalue) {
              return null;
            }
          });
        }

        ikey++;
      }
    }

    return list;
  }

  private FastSparseSet<Integer>[] ensureCapacity(int index, int size, boolean exact) {

    FastSparseSet<Integer>[] arr = elements[index];
    int[] arrnext = next[index];

    int minsize = size;
    if (!exact) {
      minsize = 2 * arr.length / 3 + 1;
      if (size > minsize) {
        minsize = size;
      }
    }

    @SuppressWarnings("unchecked") FastSparseSet<Integer>[] arrnew = new FastSparseSet[minsize];
    System.arraycopy(arr, 0, arrnew, 0, arr.length);

    int[] arrnextnew = new int[minsize];
    System.arraycopy(arrnext, 0, arrnextnew, 0, arrnext.length);

    elements[index] = arrnew;
    next[index] = arrnextnew;

    return arrnew;
  }
}