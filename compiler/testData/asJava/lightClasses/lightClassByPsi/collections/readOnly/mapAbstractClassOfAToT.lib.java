public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ATMap /* test.ATMap*/<T>  implements java.util.Map<test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ATMap();//  .ctor()

  public T compute(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>);//  compute(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>)

  public T computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends T>);//  computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends T>)

  public T computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>);//  computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>)

  public T merge(test.A, T, java.util.function.BiFunction<? super T, ? super T, ? extends T>);//  merge(test.A, T, java.util.function.BiFunction<? super T, ? super T, ? extends T>)

  public T put(test.A, T);//  put(test.A, T)

  public T putIfAbsent(test.A, T);//  putIfAbsent(test.A, T)

  public T remove(java.lang.Object);//  remove(java.lang.Object)

  public T replace(test.A, T);//  replace(test.A, T)

  public abstract T get(test.A);//  get(test.A)

  public abstract boolean containsKey(test.A);//  containsKey(test.A)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<T> getValues();//  getValues()

  public abstract java.util.Set<java.util.Map.Entry<test.A, T>> getEntries();//  getEntries()

  public abstract java.util.Set<test.A> getKeys();//  getKeys()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(test.A, T, T);//  replace(test.A, T, T)

  public final T get(java.lang.Object);//  get(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<T> values();//  values()

  public final java.util.Set<java.util.Map.Entry<test.A, T>> entrySet();//  entrySet()

  public final java.util.Set<test.A> keySet();//  keySet()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends test.A, ? extends T>);//  putAll(java.util.Map<? extends test.A, ? extends T>)

  public void replaceAll(java.util.function.BiFunction<? super test.A, ? super T, ? extends T>);//  replaceAll(java.util.function.BiFunction<? super test.A, ? super T, ? extends T>)
}

public abstract class ATMap2 /* test.ATMap2*/<T>  implements java.util.Map<test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<T> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, T>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public T get(@org.jetbrains.annotations.NotNull() test.A);//  get(test.A)

  public  ATMap2();//  .ctor()

  public T compute(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>);//  compute(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>)

  public T computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends T>);//  computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends T>)

  public T computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>);//  computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super T, ? extends T>)

  public T merge(test.A, T, java.util.function.BiFunction<? super T, ? super T, ? extends T>);//  merge(test.A, T, java.util.function.BiFunction<? super T, ? super T, ? extends T>)

  public T put(test.A, T);//  put(test.A, T)

  public T putIfAbsent(test.A, T);//  putIfAbsent(test.A, T)

  public T remove(java.lang.Object);//  remove(java.lang.Object)

  public T replace(test.A, T);//  replace(test.A, T)

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(test.A, T, T);//  replace(test.A, T, T)

  public final T get(java.lang.Object);//  get(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<T> values();//  values()

  public final java.util.Set<java.util.Map.Entry<test.A, T>> entrySet();//  entrySet()

  public final java.util.Set<test.A> keySet();//  keySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends test.A, ? extends T>);//  putAll(java.util.Map<? extends test.A, ? extends T>)

  public void replaceAll(java.util.function.BiFunction<? super test.A, ? super T, ? extends T>);//  replaceAll(java.util.function.BiFunction<? super test.A, ? super T, ? extends T>)
}
