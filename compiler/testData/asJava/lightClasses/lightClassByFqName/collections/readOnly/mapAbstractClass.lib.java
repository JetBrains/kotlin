public abstract class CMap /* test.CMap*/<KElem, VElem>  implements java.util.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get(java.lang.Object);//  get(java.lang.Object)

  public  CMap();//  .ctor()

  public VElem compute(KElem, java.util.function.BiFunction<? super KElem, ? super VElem, ? extends VElem>);//  compute(KElem, java.util.function.BiFunction<? super KElem, ? super VElem, ? extends VElem>)

  public VElem computeIfAbsent(KElem, java.util.function.Function<? super KElem, ? extends VElem>);//  computeIfAbsent(KElem, java.util.function.Function<? super KElem, ? extends VElem>)

  public VElem computeIfPresent(KElem, java.util.function.BiFunction<? super KElem, ? super VElem, ? extends VElem>);//  computeIfPresent(KElem, java.util.function.BiFunction<? super KElem, ? super VElem, ? extends VElem>)

  public VElem merge(KElem, VElem, java.util.function.BiFunction<? super VElem, ? super VElem, ? extends VElem>);//  merge(KElem, VElem, java.util.function.BiFunction<? super VElem, ? super VElem, ? extends VElem>)

  public VElem put(KElem, VElem);//  put(KElem, VElem)

  public VElem putIfAbsent(KElem, VElem);//  putIfAbsent(KElem, VElem)

  public VElem remove(java.lang.Object);//  remove(java.lang.Object)

  public VElem replace(KElem, VElem);//  replace(KElem, VElem)

  public boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object, java.lang.Object);//  remove(java.lang.Object, java.lang.Object)

  public boolean replace(KElem, VElem, VElem);//  replace(KElem, VElem, VElem)

  public final int size();//  size()

  public final java.util.Collection<VElem> values();//  values()

  public final java.util.Set<KElem> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<KElem, VElem>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(java.util.Map<? extends KElem, ? extends VElem>);//  putAll(java.util.Map<? extends KElem, ? extends VElem>)

  public void replaceAll(java.util.function.BiFunction<? super KElem, ? super VElem, ? extends VElem>);//  replaceAll(java.util.function.BiFunction<? super KElem, ? super VElem, ? extends VElem>)
}
