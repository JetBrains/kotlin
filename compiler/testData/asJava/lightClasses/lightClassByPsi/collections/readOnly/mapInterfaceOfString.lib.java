public abstract class CMap /* test.CMap*/ implements test.IMap {
  public  CMap();//  .ctor()

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.lang.String get(java.lang.String);//  get(java.lang.String)

  public abstract java.util.Collection<java.lang.String> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, java.lang.String, java.lang.String);//  replace(java.lang.String, java.lang.String, java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()

  public java.lang.String compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.String>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String merge(java.lang.String, java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  merge(java.lang.String, java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String put(java.lang.String, java.lang.String);//  put(java.lang.String, java.lang.String)

  public java.lang.String putIfAbsent(java.lang.String, java.lang.String);//  putIfAbsent(java.lang.String, java.lang.String)

  public java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.String replace(java.lang.String, java.lang.String);//  replace(java.lang.String, java.lang.String)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)
}

public abstract class CMap2 /* test.CMap2*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.String get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  public  CMap2(@org.jetbrains.annotations.NotNull() test.IMap);//  .ctor(test.IMap)

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(java.lang.String)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, java.lang.String, java.lang.String);//  replace(java.lang.String, java.lang.String, java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public java.lang.String compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.String>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String merge(java.lang.String, java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  merge(java.lang.String, java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String put(java.lang.String, java.lang.String);//  put(java.lang.String, java.lang.String)

  public java.lang.String putIfAbsent(java.lang.String, java.lang.String);//  putIfAbsent(java.lang.String, java.lang.String)

  public java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.String replace(java.lang.String, java.lang.String);//  replace(java.lang.String, java.lang.String)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)
}

public class CMap3 /* test.CMap3*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.String get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  public  CMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(java.lang.String)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, java.lang.String, java.lang.String);//  replace(java.lang.String, java.lang.String, java.lang.String)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public java.lang.String compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.String>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String merge(java.lang.String, java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  merge(java.lang.String, java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)

  public java.lang.String put(java.lang.String, java.lang.String);//  put(java.lang.String, java.lang.String)

  public java.lang.String putIfAbsent(java.lang.String, java.lang.String);//  putIfAbsent(java.lang.String, java.lang.String)

  public java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.String replace(java.lang.String, java.lang.String);//  replace(java.lang.String, java.lang.String)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.String, ? extends java.lang.String>)
}

public abstract interface IMap /* test.IMap*/ extends java.util.Map<java.lang.String, java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
