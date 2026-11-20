public abstract class AnyBoundedList /* test.AnyBoundedList*/<E>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  AnyBoundedList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public E remove(int);//  remove(int)

  public E set(int, E);//  set(int, E)

  public abstract int getSize();//  getSize()

  public boolean add(E);//  add(E)

  public boolean addAll(int, java.util.Collection<? extends E>);//  addAll(int, java.util.Collection<? extends E>)

  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.List<E> subList(int, int);//  subList(int, int)

  public java.util.ListIterator<E> listIterator();//  listIterator()

  public java.util.ListIterator<E> listIterator(int);//  listIterator(int)

  public void add(int, E);//  add(int, E)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<E>);//  replaceAll(java.util.function.UnaryOperator<E>)

  public void sort(java.util.Comparator<? super E>);//  sort(java.util.Comparator<? super E>)
}

public abstract class BoundedCollection /* test.BoundedCollection*/<E extends java.lang.CharSequence>  implements java.util.Collection<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  BoundedCollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(E);//  contains(E)

  public abstract int getSize();//  getSize()

  public boolean add(E);//  add(E)

  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super E>);//  removeIf(java.util.function.Predicate<? super E>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<E> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class BoundedList /* test.BoundedList*/<E extends java.lang.CharSequence>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  BoundedList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public E remove(int);//  remove(int)

  public E set(int, E);//  set(int, E)

  public abstract boolean contains(E);//  contains(E)

  public abstract int getSize();//  getSize()

  public abstract int indexOf(E);//  indexOf(E)

  public abstract int lastIndexOf(E);//  lastIndexOf(E)

  public boolean add(E);//  add(E)

  public boolean addAll(int, java.util.Collection<? extends E>);//  addAll(int, java.util.Collection<? extends E>)

  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.List<E> subList(int, int);//  subList(int, int)

  public java.util.ListIterator<E> listIterator();//  listIterator()

  public java.util.ListIterator<E> listIterator(int);//  listIterator(int)

  public void add(int, E);//  add(int, E)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<E>);//  replaceAll(java.util.function.UnaryOperator<E>)

  public void sort(java.util.Comparator<? super E>);//  sort(java.util.Comparator<? super E>)
}

public abstract class BoundedListOverrides /* test.BoundedListOverrides*/<E extends java.lang.CharSequence>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  BoundedListOverrides();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public E remove(int);//  remove(int)

  public E set(int, E);//  set(int, E)

  public abstract int getSize();//  getSize()

  public abstract int lastIndexOf(E);//  lastIndexOf(E)

  public boolean add(E);//  add(E)

  public boolean addAll(int, java.util.Collection<? extends E>);//  addAll(int, java.util.Collection<? extends E>)

  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  public boolean contains(@org.jetbrains.annotations.NotNull() E);//  contains(E)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public int indexOf(@org.jetbrains.annotations.NotNull() E);//  indexOf(E)

  public java.lang.Object[] toArray();//  toArray()

  public java.util.List<E> subList(int, int);//  subList(int, int)

  public java.util.ListIterator<E> listIterator();//  listIterator()

  public java.util.ListIterator<E> listIterator(int);//  listIterator(int)

  public void add(int, E);//  add(int, E)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<E>);//  replaceAll(java.util.function.UnaryOperator<E>)

  public void sort(java.util.Comparator<? super E>);//  sort(java.util.Comparator<? super E>)
}

public abstract class BoundedMapOverrides /* test.BoundedMapOverrides*/<K extends java.lang.CharSequence, V extends java.lang.Number>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public V getOrDefault(@org.jetbrains.annotations.NotNull() K, @org.jetbrains.annotations.NotNull() V);//  getOrDefault(K, V)

  @org.jetbrains.annotations.Nullable()
  public V get(@org.jetbrains.annotations.NotNull() K);//  get(K)

  public  BoundedMapOverrides();//  .ctor()

  public V compute(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>);//  compute(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>)

  public V computeIfAbsent(K, java.util.function.Function<? super K, ? extends V>);//  computeIfAbsent(K, java.util.function.Function<? super K, ? extends V>)

  public V computeIfPresent(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>);//  computeIfPresent(K, java.util.function.BiFunction<? super K, ? super V, ? extends V>)

  public V merge(K, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>);//  merge(K, V, java.util.function.BiFunction<? super V, ? super V, ? extends V>)

  public V put(K, V);//  put(K, V)

  public V putIfAbsent(K, V);//  putIfAbsent(K, V)

  public V remove(java.lang.Object);//  remove(java.lang.Object)

  public V replace(K, V);//  replace(K, V)

  public abstract boolean containsKey(K);//  containsKey(K)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<K> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<K, V>> getEntries();//  getEntries()

  public boolean containsValue(@org.jetbrains.annotations.NotNull() V);//  containsValue(V)

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(K, V, V);//  replace(K, V, V)

  public final V get(java.lang.Object);//  get(java.lang.Object)

  public final V getOrDefault(java.lang.Object, V);//  getOrDefault(java.lang.Object, V)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<K> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<K, V>> entrySet();//  entrySet()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends K, ? extends V>);//  putAll(java.util.Map<? extends K, ? extends V>)

  public void replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V>);//  replaceAll(java.util.function.BiFunction<? super K, ? super V, ? extends V>)
}

public abstract class BoundedMutableListOverrides /* test.BoundedMutableListOverrides*/<E extends java.lang.CharSequence>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMutableList {
  public  BoundedMutableListOverrides();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract E removeAt(int);//  removeAt(int)

  public abstract boolean contains(E);//  contains(E)

  public abstract int getSize();//  getSize()

  public abstract int indexOf(E);//  indexOf(E)

  public abstract int lastIndexOf(E);//  lastIndexOf(E)

  public boolean remove(@org.jetbrains.annotations.NotNull() E);//  remove(E)

  public final E remove(int);//  remove(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class BoundedMutableMapOverrides /* test.BoundedMutableMapOverrides*/<K extends java.lang.CharSequence, V>  implements java.util.Map<K, V>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.Nullable()
  public V remove(@org.jetbrains.annotations.NotNull() K);//  remove(K)

  public  BoundedMutableMapOverrides();//  .ctor()

  public abstract V get(K);//  get(K)

  public abstract boolean containsKey(K);//  containsKey(K)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<V> getValues();//  getValues()

  public abstract java.util.Set<K> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<K, V>> getEntries();//  getEntries()

  public boolean remove(@org.jetbrains.annotations.NotNull() K, V);//  remove(K, V)

  public final V get(java.lang.Object);//  get(java.lang.Object)

  public final V remove(java.lang.Object);//  remove(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<V> values();//  values()

  public final java.util.Set<K> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<K, V>> entrySet();//  entrySet()
}

public abstract class TransitivelyBoundedList /* test.TransitivelyBoundedList*/<T extends java.lang.CharSequence, E extends T>  implements java.util.List<E>, kotlin.jvm.internal.markers.KMappedMarker {
  public  TransitivelyBoundedList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public E remove(int);//  remove(int)

  public E set(int, E);//  set(int, E)

  public abstract boolean contains(E);//  contains(E)

  public abstract int getSize();//  getSize()

  public abstract int indexOf(E);//  indexOf(E)

  public abstract int lastIndexOf(E);//  lastIndexOf(E)

  public boolean add(E);//  add(E)

  public boolean addAll(int, java.util.Collection<? extends E>);//  addAll(int, java.util.Collection<? extends E>)

  public boolean addAll(java.util.Collection<? extends E>);//  addAll(java.util.Collection<? extends E>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.List<E> subList(int, int);//  subList(int, int)

  public java.util.ListIterator<E> listIterator();//  listIterator()

  public java.util.ListIterator<E> listIterator(int);//  listIterator(int)

  public void add(int, E);//  add(int, E)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<E>);//  replaceAll(java.util.function.UnaryOperator<E>)

  public void sort(java.util.Comparator<? super E>);//  sort(java.util.Comparator<? super E>)
}
