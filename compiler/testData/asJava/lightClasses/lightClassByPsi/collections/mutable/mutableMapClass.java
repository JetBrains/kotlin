public abstract class CMutableMap /* test.CMutableMap*/<KElem, VElem>  implements java.util.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  public  CMutableMap();//  .ctor()

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<VElem> getValues();//  getValues()

  public abstract java.util.Set<KElem> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  public final int size();//  size()

  public final java.util.Collection<VElem> values();//  values()

  public final java.util.Set<KElem> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<KElem, VElem>> entrySet();//  entrySet()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/<KElem, VElem>  implements java.util.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(KElem, VElem);//  put(KElem, VElem)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableMap2();//  .ctor()

  public VElem get(java.lang.Object);//  get(java.lang.Object)

  public VElem remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<VElem> values();//  values()

  public final java.util.Set<KElem> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<KElem, VElem>> entrySet();//  entrySet()

  public void putAll(java.util.Map<? extends KElem, ? extends VElem>);//  putAll(java.util.Map<? extends KElem, ? extends VElem>)
}
