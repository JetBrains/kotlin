// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class VBStyleCollection<E, K> extends ArrayList<E> {

  private HashMap<K, Integer> map = new HashMap<>();

  private ArrayList<K> lstKeys = new ArrayList<>();

  public VBStyleCollection() {
    super();
  }

  public VBStyleCollection(int initialCapacity) {
    super(initialCapacity);
    lstKeys = new ArrayList<>(initialCapacity);
    map = new HashMap<>(initialCapacity);
  }

  @Override
  public boolean add(E element) {
    lstKeys.add(null);
    super.add(element);
    return true;
  }

  @Override
  public boolean remove(Object element) {   // TODO: error on void remove(E element)
    throw new RuntimeException("not implemented!");
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    for (int i = c.size() - 1; i >= 0; i--) {
      lstKeys.add(null);
    }
    return super.addAll(c);
  }

  public void addAllWithKey(Collection<E> elements, Collection<K> keys) {
    int index = super.size();

    for (K key : keys) {
      map.put(key, index++);
    }

    super.addAll(elements);
    lstKeys.addAll(keys);
  }

  public void addWithKey(E element, K key) {
    map.put(key, super.size());
    super.add(element);
    lstKeys.add(key);
  }

  // TODO: speed up the method
  public E putWithKey(E element, K key) {
    Integer index = map.get(key);
    if (index == null) {
      addWithKey(element, key);
    }
    else {
      return super.set(index, element);
    }
    return null;
  }

  @Override
  public void add(int index, E element) {
    addToListIndex(index, 1);
    lstKeys.add(index, null);
    super.add(index, element);
  }

  public void addWithKeyAndIndex(int index, E element, K key) {
    addToListIndex(index, 1);
    map.put(key, index);
    super.add(index, element);
    lstKeys.add(index, key);
  }

  public void removeWithKey(K key) {
    Integer indexBox = map.get(key);
    if (indexBox == null) {
      return;
    }
    int index = indexBox;
    addToListIndex(index + 1, -1);
    super.remove(index);
    lstKeys.remove(index);
    map.remove(key);
  }

  @Override
  public E remove(int index) {
    addToListIndex(index + 1, -1);
    K obj = lstKeys.get(index);
    if (obj != null) {
      map.remove(obj);
    }
    lstKeys.remove(index);
    return super.remove(index);
  }

  public E getWithKey(K key) {
    Integer index = map.get(key);
    if (index == null) {
      return null;
    }
    return super.get(index);
  }

  public int getIndexByKey(K key) {
    return map.get(key);
  }

  public E getLast() {
    return super.get(super.size() - 1);
  }

  public boolean containsKey(K key) {
    return map.containsKey(key);
  }

  @Override
  public void clear() {
    map.clear();
    lstKeys.clear();
    super.clear();
  }

  @Override
  public VBStyleCollection<E, K> clone() {
    VBStyleCollection<E, K> c = new VBStyleCollection<>();
    c.addAll(new ArrayList<>(this));
    c.setMap(new HashMap<>(map));
    c.setLstKeys(new ArrayList<>(lstKeys));
    return c;
  }

  public void setMap(HashMap<K, Integer> map) {
    this.map = map;
  }

  public K getKey(int index) {
    return lstKeys.get(index);
  }

  public ArrayList<K> getLstKeys() {
    return lstKeys;
  }

  public void setLstKeys(ArrayList<K> lstKeys) {
    this.lstKeys = lstKeys;
  }

  private void addToListIndex(int index, int diff) {
    for (int i = lstKeys.size() - 1; i >= index; i--) {
      K obj = lstKeys.get(i);
      if (obj != null) {
        map.put(obj, i + diff);
      }
    }
  }

  public String toStringVb() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < super.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(this.getLstKeys().get(i))
        .append("=")
        .append(super.get(i));
    }
    sb.append("]");
    return sb.toString();
  }
}