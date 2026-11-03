public abstract class AnnotatedValueMap /* test.AnnotatedValueMap*/ implements java.util.Map<java.lang.String, java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
  public  AnnotatedValueMap();//  .ctor()

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract boolean containsValue(int);//  containsValue(int)

  public abstract int getSize();//  getSize()

  public abstract java.lang.Integer get(java.lang.String);//  get(java.lang.String)

  public abstract java.util.Collection<java.lang.Integer> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.Integer>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, java.lang.Integer, java.lang.Integer);//  replace(java.lang.String, java.lang.Integer, java.lang.Integer)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.Integer>> entrySet();//  entrySet()

  public java.lang.Integer compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.Integer>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer merge(java.lang.String, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  merge(java.lang.String, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer put(java.lang.String, int);//  put(java.lang.String, int)

  public java.lang.Integer putIfAbsent(java.lang.String, java.lang.Integer);//  putIfAbsent(java.lang.String, java.lang.Integer)

  public java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.Integer replace(java.lang.String, java.lang.Integer);//  replace(java.lang.String, java.lang.Integer)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.Integer>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>)
}

public abstract class NotNullGetMap /* test.NotNullGetMap*/ implements java.util.Map<java.lang.String, java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  public  NotNullGetMap();//  .ctor()

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract boolean containsValue(int);//  containsValue(int)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<java.lang.Integer> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.Integer>> getEntries();//  getEntries()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, java.lang.Integer, java.lang.Integer);//  replace(java.lang.String, java.lang.Integer, java.lang.Integer)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.Integer>> entrySet();//  entrySet()

  public java.lang.Integer compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.Integer>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends java.lang.Integer>)

  public java.lang.Integer computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer merge(java.lang.String, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>);//  merge(java.lang.String, java.lang.Integer, java.util.function.BiFunction<? super java.lang.Integer, ? super java.lang.Integer, ? extends java.lang.Integer>)

  public java.lang.Integer put(java.lang.String, int);//  put(java.lang.String, int)

  public java.lang.Integer putIfAbsent(java.lang.String, java.lang.Integer);//  putIfAbsent(java.lang.String, java.lang.Integer)

  public java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public java.lang.Integer replace(java.lang.String, java.lang.Integer);//  replace(java.lang.String, java.lang.Integer)

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.Integer>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super java.lang.Integer, ? extends java.lang.Integer>)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE_USE})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.TYPE})
public abstract @interface TypeAnn /* test.TypeAnn*/ {
}
