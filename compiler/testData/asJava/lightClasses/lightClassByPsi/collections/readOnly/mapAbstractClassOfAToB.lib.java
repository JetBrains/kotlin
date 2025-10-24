public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMap /* test.ABMap*/ implements java.util.Map<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ABMap();//  .ctor()

  public abstract boolean containsKey(test.A);//  containsKey(test.A)

  public abstract boolean containsValue(test.B);//  containsValue(test.B)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<test.B> getValues();//  getValues()

  public abstract java.util.Set<java.util.Map.Entry<test.A, test.B>> getEntries();//  getEntries()

  public abstract java.util.Set<test.A> getKeys();//  getKeys()

  public abstract test.B get(test.A);//  get(test.A)

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(test.A, test.B, test.B);//  replace(test.A, test.B, test.B)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<test.B> values();//  values()

  public final java.util.Set<java.util.Map.Entry<test.A, test.B>> entrySet();//  entrySet()

  public final java.util.Set<test.A> keySet();//  keySet()

  public final test.B get(java.lang.Object);//  get(java.lang.Object)

  public test.B compute(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>);//  compute(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>)

  public test.B computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends test.B>);//  computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends test.B>)

  public test.B computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>);//  computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>)

  public test.B merge(test.A, test.B, java.util.function.BiFunction<? super test.B, ? super test.B, ? extends test.B>);//  merge(test.A, test.B, java.util.function.BiFunction<? super test.B, ? super test.B, ? extends test.B>)

  public test.B put(test.A, test.B);//  put(test.A, test.B)

  public test.B putIfAbsent(test.A, test.B);//  putIfAbsent(test.A, test.B)

  public test.B remove(java.lang.Object);//  remove(java.lang.Object)

  public test.B replace(test.A, test.B);//  replace(test.A, test.B)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends test.A, ? extends test.B>);//  putAll(java.util.Map<? extends test.A, ? extends test.B>)

  public void replaceAll(java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>);//  replaceAll(java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>)
}

public abstract class ABMap2 /* test.ABMap2*/ implements java.util.Map<test.A, test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<test.A, test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public test.B get(@org.jetbrains.annotations.NotNull() test.A);//  get(test.A)

  public  ABMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() test.A);//  containsKey(test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() test.B);//  containsValue(test.B)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(test.A, test.B, test.B);//  replace(test.A, test.B, test.B)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<test.B> values();//  values()

  public final java.util.Set<java.util.Map.Entry<test.A, test.B>> entrySet();//  entrySet()

  public final java.util.Set<test.A> keySet();//  keySet()

  public final test.B get(java.lang.Object);//  get(java.lang.Object)

  public int getSize();//  getSize()

  public test.B compute(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>);//  compute(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>)

  public test.B computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends test.B>);//  computeIfAbsent(test.A, java.util.function.Function<? super test.A, ? extends test.B>)

  public test.B computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>);//  computeIfPresent(test.A, java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>)

  public test.B merge(test.A, test.B, java.util.function.BiFunction<? super test.B, ? super test.B, ? extends test.B>);//  merge(test.A, test.B, java.util.function.BiFunction<? super test.B, ? super test.B, ? extends test.B>)

  public test.B put(test.A, test.B);//  put(test.A, test.B)

  public test.B putIfAbsent(test.A, test.B);//  putIfAbsent(test.A, test.B)

  public test.B remove(java.lang.Object);//  remove(java.lang.Object)

  public test.B replace(test.A, test.B);//  replace(test.A, test.B)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends test.A, ? extends test.B>);//  putAll(java.util.Map<? extends test.A, ? extends test.B>)

  public void replaceAll(java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>);//  replaceAll(java.util.function.BiFunction<? super test.A, ? super test.B, ? extends test.B>)
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
