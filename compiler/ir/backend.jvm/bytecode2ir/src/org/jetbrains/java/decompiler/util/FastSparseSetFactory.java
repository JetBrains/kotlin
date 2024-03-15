// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import java.util.*;

public class FastSparseSetFactory<E> {

  private final VBStyleCollection<int[], E> colValuesInternal = new VBStyleCollection<>();

  private int lastBlock;

  private int lastMask;

  public FastSparseSetFactory(Collection<? extends E> set) {

    int block = -1;
    int mask = -1;
    int index = 0;

    for (E element : set) {

      block = index / 32;

      if (index % 32 == 0) {
        mask = 1;
      }
      else {
        mask <<= 1;
      }

      colValuesInternal.putWithKey(new int[]{block, mask}, element);

      index++;
    }

    lastBlock = block;
    lastMask = mask;
  }

  private int[] addElement(E element) {

    if (lastMask == -1 || lastMask == 0x80000000) {
      lastMask = 1;
      lastBlock++;
    }
    else {
      lastMask <<= 1;
    }

    int[] pointer = new int[]{lastBlock, lastMask};
    colValuesInternal.putWithKey(pointer, element);

    return pointer;
  }

  public FastSparseSet<E> createEmptySet() {
    return new FastSparseSet<>(this);
  }

  public int getLastBlock() {
    return lastBlock;
  }

  private VBStyleCollection<int[], E> getInternalValuesCollection() {
    return colValuesInternal;
  }


  public static final class FastSparseSet<E> implements Iterable<E> {
    public static final FastSparseSet[] EMPTY_ARRAY = new FastSparseSet[0];

    private final FastSparseSetFactory<E> factory;

    private final VBStyleCollection<int[], E> colValuesInternal;

    private int[] data;
    private int[] next;

    private FastSparseSet(FastSparseSetFactory<E> factory) {
      this.factory = factory;
      this.colValuesInternal = factory.getInternalValuesCollection();

      int length = factory.getLastBlock() + 1;
      this.data = new int[length];
      this.next = new int[length];
    }

    private FastSparseSet(FastSparseSetFactory<E> factory, int[] data, int[] next) {
      this.factory = factory;
      this.colValuesInternal = factory.getInternalValuesCollection();

      this.data = data;
      this.next = next;
    }

    public FastSparseSet<E> getCopy() {
      int[] newData = new int[this.data.length];
      int[] newNext = new int[this.next.length];
      System.arraycopy(this.data, 0, newData, 0, newData.length);
      System.arraycopy(this.next, 0, newNext, 0, newNext.length);

      return new FastSparseSet<>(factory, newData, newNext);
    }

    private int[] ensureCapacity(int index) {

      int newlength = data.length;
      if (newlength == 0) {
        newlength = 1;
      }

      while (newlength <= index) {
        newlength *= 2;
      }

      data = Arrays.copyOf(data, newlength);
      next = Arrays.copyOf(next, newlength);

      return data;
    }

    public void add(E element) {
      int[] index = colValuesInternal.getWithKey(element);

      if (index == null) {
        index = factory.addElement(element);
      }

      int block = index[0];
      if (block >= data.length) {
        ensureCapacity(block);
      }

      data[block] |= index[1];

      changeNext(next, block, next[block], block);
    }

    public void remove(E element) {
      int[] index = colValuesInternal.getWithKey(element);

      if (index == null) {
        index = factory.addElement(element);
      }

      int block = index[0];
      if (block < data.length) {
        data[block] &= ~index[1];

        if (data[block] == 0) {
          changeNext(next, block, block, next[block]);
        }
      }
    }

    public boolean contains(E element) {
      int[] index = colValuesInternal.getWithKey(element);

      if (index == null) {
        index = factory.addElement(element);
      }

      return index[0] < data.length && ((data[index[0]] & index[1]) != 0);
    }

    private void setNext() {

      int link = 0;
      for (int i = data.length - 1; i >= 0; i--) {
        next[i] = link;
        if (data[i] != 0) {
          link = i;
        }
      }
    }

    private static void changeNext(int[] arrnext, int key, int oldnext, int newnext) {
      for (int i = key - 1; i >= 0; i--) {
        if (arrnext[i] == oldnext) {
          arrnext[i] = newnext;
        }
        else {
          break;
        }
      }
    }

    public void union(FastSparseSet<E> set) {

      int[] extdata = set.getData();
      int[] extnext = set.getNext();
      int[] intdata = data;
      int intlength = intdata.length;

      int pointer = 0;
      do {
        if (pointer >= intlength) {
          intdata = ensureCapacity(extdata.length - 1);
        }

        boolean nextrec = (intdata[pointer] == 0);
        intdata[pointer] |= extdata[pointer];

        if (nextrec) {
          changeNext(next, pointer, next[pointer], pointer);
        }

        pointer = extnext[pointer];
      }
      while (pointer != 0);
    }

    public void intersection(FastSparseSet<E> set) {
      int[] extdata = set.getData();
      int[] intdata = data;

      int minlength = Math.min(extdata.length, intdata.length);

      for (int i = minlength - 1; i >= 0; i--) {
        intdata[i] &= extdata[i];
      }

      for (int i = intdata.length - 1; i >= minlength; i--) {
        intdata[i] = 0;
      }

      setNext();
    }

    public void complement(FastSparseSet<E> set) {

      int[] extdata = set.getData();
      int[] intdata = data;
      int extlength = extdata.length;

      int pointer = 0;
      do {
        if (pointer >= extlength) {
          break;
        }

        intdata[pointer] &= ~extdata[pointer];
        if (intdata[pointer] == 0) {
          changeNext(next, pointer, pointer, next[pointer]);
        }

        pointer = next[pointer];
      }
      while (pointer != 0);
    }


    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof FastSparseSet)) return false;

      int[] longdata = ((FastSparseSet)o).getData();
      int[] shortdata = data;

      if (data.length > longdata.length) {
        shortdata = longdata;
        longdata = data;
      }

      for (int i = shortdata.length - 1; i >= 0; i--) {
        if (shortdata[i] != longdata[i]) {
          return false;
        }
      }

      for (int i = longdata.length - 1; i >= shortdata.length; i--) {
        if (longdata[i] != 0) {
          return false;
        }
      }

      return true;
    }

    public int getCardinality() {

      boolean found = false;
      int[] intdata = data;

      for (int i = intdata.length - 1; i >= 0; i--) {
        int block = intdata[i];
        if (block != 0) {
          if (found) {
            return 2;
          }
          else {
            if ((block & (block - 1)) == 0) {
              found = true;
            }
            else {
              return 2;
            }
          }
        }
      }

      return found ? 1 : 0;
    }

    public boolean isEmpty() {
      return data.length == 0 || (next[0] == 0 && data[0] == 0);
    }

    @Override
    public Iterator<E> iterator() {
      return new FastSparseSetIterator<>(this);
    }

    public Set<E> toPlainSet() {
      HashSet<E> set = new HashSet<>();

      int[] intdata = data;

      int size = data.length * 32;
      if (size > colValuesInternal.size()) {
        size = colValuesInternal.size();
      }

      for (int i = size - 1; i >= 0; i--) {
        int[] index = colValuesInternal.get(i);

        if ((intdata[index[0]] & index[1]) != 0) {
          set.add(colValuesInternal.getKey(i));
        }
      }

      return set;
    }

    public String toString() {
      return toPlainSet().toString();
    }

    private int[] getData() {
      return data;
    }

    private int[] getNext() {
      return next;
    }

    public FastSparseSetFactory<E> getFactory() {
      return factory;
    }
  }

  public static final class FastSparseSetIterator<E> implements Iterator<E> {

    private final VBStyleCollection<int[], E> colValuesInternal;
    private final int[] data;
    private final int[] next;
    private final int size;

    private int pointer = -1;
    private int next_pointer = -1;

    private FastSparseSetIterator(FastSparseSet<E> set) {
      colValuesInternal = set.getFactory().getInternalValuesCollection();
      data = set.getData();
      next = set.getNext();
      size = colValuesInternal.size();
    }

    private int getNextIndex(int index) {

      index++;
      int bindex = index >>> 5;
      int dindex = index & 0x1F;

      while (bindex < data.length) {
        int block = data[bindex];

        if (block != 0) {
          block >>>= dindex;
          while (dindex < 32) {
            if ((block & 1) != 0) {
              return (bindex << 5) + dindex;
            }
            block >>>= 1;
            dindex++;
          }
        }

        dindex = 0;
        bindex = next[bindex];

        if (bindex == 0) {
          break;
        }
      }

      return -1;
    }

    @Override
    public boolean hasNext() {
      next_pointer = getNextIndex(pointer);
      return (next_pointer >= 0);
    }

    @Override
    public E next() {
      if (next_pointer >= 0) {
        pointer = next_pointer;
      }
      else {
        pointer = getNextIndex(pointer);
        if (pointer == -1) {
          pointer = size;
        }
      }

      next_pointer = -1;
      return pointer < size ? colValuesInternal.getKey(pointer) : null;
    }

    @Override
    public void remove() {
      int[] index = colValuesInternal.get(pointer);
      data[index[0]] &= ~index[1];
    }
  }
}

