public abstract class SMap /* test.SMap*/<VElem>  implements java.util.Map<java.lang.String, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  public  SMap();//  .ctor()

  public VElem compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super VElem, ? extends VElem>);//  compute(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super VElem, ? extends VElem>)

  public VElem computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends VElem>);//  computeIfAbsent(java.lang.String, java.util.function.Function<? super java.lang.String, ? extends VElem>)

  public VElem computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super VElem, ? extends VElem>);//  computeIfPresent(java.lang.String, java.util.function.BiFunction<? super java.lang.String, ? super VElem, ? extends VElem>)

  public VElem merge(java.lang.String, VElem, java.util.function.BiFunction<? super VElem, ? super VElem, ? extends VElem>);//  merge(java.lang.String, VElem, java.util.function.BiFunction<? super VElem, ? super VElem, ? extends VElem>)

  public VElem put(java.lang.String, VElem);//  put(java.lang.String, VElem)

  public VElem putIfAbsent(java.lang.String, VElem);//  putIfAbsent(java.lang.String, VElem)

  public VElem remove(java.lang.Object);//  remove(java.lang.Object)

  public VElem replace(java.lang.String, VElem);//  replace(java.lang.String, VElem)

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(java.lang.String, VElem, VElem);//  replace(java.lang.String, VElem, VElem)

  public final VElem get(java.lang.Object);//  get(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<VElem> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, VElem>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends java.lang.String, ? extends VElem>);//  putAll(java.util.Map<? extends java.lang.String, ? extends VElem>)

  public void replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super VElem, ? extends VElem>);//  replaceAll(java.util.function.BiFunction<? super java.lang.String, ? super VElem, ? extends VElem>)
}
