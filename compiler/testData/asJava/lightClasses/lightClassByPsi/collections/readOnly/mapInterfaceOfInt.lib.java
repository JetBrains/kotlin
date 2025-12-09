public abstract class CMap /* test.CMap*/ implements test.IMap {
  public  CMap();//  .ctor()

  public abstract boolean containsKey(int);//  containsKey(int)

  public abstract boolean containsValue(int);//  containsValue(int)

  public abstract int getSize();//  getSize()

  public abstract java.lang.Integer get(int);//  get(int)

  public abstract java.util.Collection<java.lang.Integer> getValues();//  getValues()

  public abstract java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.Integer, java.lang.Integer, java.lang.Integer);//  replace(java.lang.Integer, java.lang.Integer, java.lang.Integer)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.Integer> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> entrySet();//  entrySet()

  public java.lang.Integer compute(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  compute(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfAbsent(java.lang.Integer, java.util.function.Function<? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfAbsent(java.lang.Integer, java.util.function.Function<? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfPresent(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfPresent(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer merge(java.lang.Integer, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  merge(java.lang.Integer, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer put(int, int);//  put(int, int)

  public java.lang.Integer putIfAbsent(java.lang.Integer, java.lang.Integer);//  putIfAbsent(java.lang.Integer, java.lang.Integer)

  public java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.Integer replace(java.lang.Integer, java.lang.Integer);//  replace(java.lang.Integer, java.lang.Integer)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  replaceAll(java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)
}

public abstract class CMap2 /* test.CMap2*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer get(int);//  get(int)

  public  CMap2(@org.jetbrains.annotations.NotNull() test.IMap);//  .ctor(test.IMap)

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.Integer, java.lang.Integer, java.lang.Integer);//  replace(java.lang.Integer, java.lang.Integer, java.lang.Integer)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.Integer> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public java.lang.Integer compute(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  compute(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfAbsent(java.lang.Integer, java.util.function.Function<? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfAbsent(java.lang.Integer, java.util.function.Function<? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfPresent(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfPresent(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer merge(java.lang.Integer, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  merge(java.lang.Integer, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer put(int, int);//  put(int, int)

  public java.lang.Integer putIfAbsent(java.lang.Integer, java.lang.Integer);//  putIfAbsent(java.lang.Integer, java.lang.Integer)

  public java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.Integer replace(java.lang.Integer, java.lang.Integer);//  replace(java.lang.Integer, java.lang.Integer)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  replaceAll(java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)
}

public class CMap3 /* test.CMap3*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer get(int);//  get(int)

  public  CMap3();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.Integer, java.lang.Integer, java.lang.Integer);//  replace(java.lang.Integer, java.lang.Integer, java.lang.Integer)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.Integer> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public java.lang.Integer compute(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  compute(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfAbsent(java.lang.Integer, java.util.function.Function<? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfAbsent(java.lang.Integer, java.util.function.Function<? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfPresent(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfPresent(java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer merge(java.lang.Integer, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  merge(java.lang.Integer, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer put(int, int);//  put(int, int)

  public java.lang.Integer putIfAbsent(java.lang.Integer, java.lang.Integer);//  putIfAbsent(java.lang.Integer, java.lang.Integer)

  public java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.Integer replace(java.lang.Integer, java.lang.Integer);//  replace(java.lang.Integer, java.lang.Integer)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  replaceAll(java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)
}

public abstract interface IMap /* test.IMap*/ extends java.util.Map<java.lang.Integer, java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
