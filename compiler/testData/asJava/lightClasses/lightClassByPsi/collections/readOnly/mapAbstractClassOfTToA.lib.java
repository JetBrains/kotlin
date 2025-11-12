public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class TAMap /* test.TAMap*/<T>  implements java.util.Map<T, test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  public  TAMap();//  .ctor()

  public abstract boolean containsValue(test.A);//  containsValue(test.A)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<test.A> getValues();//  getValues()

  public abstract java.util.Set<T> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<T, test.A>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(T, test.A, test.A);//  replace(T, test.A, test.A)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<test.A> values();//  values()

  public final java.util.Set<T> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<T, test.A>> entrySet();//  entrySet()

  public test.A compute(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  compute(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)

  public test.A computeIfAbsent(T, java.util.function.Function<? super T, ? extends test.A>);//  computeIfAbsent(T, java.util.function.Function<? super T, ? extends test.A>)

  public test.A computeIfPresent(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  computeIfPresent(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)

  public test.A merge(T, test.A, java.util.function.BiFunction<? super test.A, ? super test.A, ? extends test.A>);//  merge(T, test.A, java.util.function.BiFunction<? super test.A, ? super test.A, ? extends test.A>)

  public test.A put(T, test.A);//  put(T, test.A)

  public test.A putIfAbsent(T, test.A);//  putIfAbsent(T, test.A)

  public test.A remove(java.lang.Object);//  remove(java.lang.Object)

  public test.A replace(T, test.A);//  replace(T, test.A)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends T, ? extends test.A>);//  putAll(java.util.Map<? extends T, ? extends test.A>)

  public void replaceAll(java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  replaceAll(java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)
}

public abstract class TAMap2 /* test.TAMap2*/<T>  implements java.util.Map<T, test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.A> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<T> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<T, test.A>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public test.A get(java.lang.Object);//  get(java.lang.Object)

  public  TAMap2();//  .ctor()

  public boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.A);//  containsValue(test.A)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(T, test.A, test.A);//  replace(T, test.A, test.A)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<test.A> values();//  values()

  public final java.util.Set<T> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<T, test.A>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public test.A compute(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  compute(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)

  public test.A computeIfAbsent(T, java.util.function.Function<? super T, ? extends test.A>);//  computeIfAbsent(T, java.util.function.Function<? super T, ? extends test.A>)

  public test.A computeIfPresent(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  computeIfPresent(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)

  public test.A merge(T, test.A, java.util.function.BiFunction<? super test.A, ? super test.A, ? extends test.A>);//  merge(T, test.A, java.util.function.BiFunction<? super test.A, ? super test.A, ? extends test.A>)

  public test.A put(T, test.A);//  put(T, test.A)

  public test.A putIfAbsent(T, test.A);//  putIfAbsent(T, test.A)

  public test.A remove(java.lang.Object);//  remove(java.lang.Object)

  public test.A replace(T, test.A);//  replace(T, test.A)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends T, ? extends test.A>);//  putAll(java.util.Map<? extends T, ? extends test.A>)

  public void replaceAll(java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  replaceAll(java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)
}

public class TAMap3 /* test.TAMap3*/<T>  implements java.util.Map<T, test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.A> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<T> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<T, test.A>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public test.A get(java.lang.Object);//  get(java.lang.Object)

  public  TAMap3();//  .ctor()

  public boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.A);//  containsValue(test.A)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(T, test.A, test.A);//  replace(T, test.A, test.A)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<test.A> values();//  values()

  public final java.util.Set<T> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<T, test.A>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public test.A compute(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  compute(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)

  public test.A computeIfAbsent(T, java.util.function.Function<? super T, ? extends test.A>);//  computeIfAbsent(T, java.util.function.Function<? super T, ? extends test.A>)

  public test.A computeIfPresent(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  computeIfPresent(T, java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)

  public test.A merge(T, test.A, java.util.function.BiFunction<? super test.A, ? super test.A, ? extends test.A>);//  merge(T, test.A, java.util.function.BiFunction<? super test.A, ? super test.A, ? extends test.A>)

  public test.A put(T, test.A);//  put(T, test.A)

  public test.A putIfAbsent(T, test.A);//  putIfAbsent(T, test.A)

  public test.A remove(java.lang.Object);//  remove(java.lang.Object)

  public test.A replace(T, test.A);//  replace(T, test.A)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends T, ? extends test.A>);//  putAll(java.util.Map<? extends T, ? extends test.A>)

  public void replaceAll(java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>);//  replaceAll(java.util.function.BiFunction<? super T, ? super test.A, ? extends test.A>)
}
