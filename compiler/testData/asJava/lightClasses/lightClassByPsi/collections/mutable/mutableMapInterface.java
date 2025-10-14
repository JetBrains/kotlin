public abstract class CMutableMap /* test.CMutableMap*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract java.util.Collection<VElem> getValues();//  getValues()

  @java.lang.Override()
  public abstract java.util.Set<KElem> getKeys();//  getKeys()

  @java.lang.Override()
  public abstract java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<VElem> values();//  values()

  @java.lang.Override()
  public final java.util.Set<KElem> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<KElem, VElem>> entrySet();//  entrySet()

  public  CMutableMap();//  .ctor()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
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
  public VElem get(java.lang.Object);//  get(java.lang.Object)

  @java.lang.Override()
  public VElem remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  @java.lang.Override()
  public boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<VElem> values();//  values()

  @java.lang.Override()
  public final java.util.Set<KElem> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<KElem, VElem>> entrySet();//  entrySet()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(java.util.Map<? extends KElem, ? extends VElem>);//  putAll(java.util.Map<? extends KElem, ? extends VElem>)

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMap<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMap<KElem, VElem>)
}

public class CMutableMap3 /* test.CMutableMap3*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
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
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(KElem, VElem);//  put(KElem, VElem)

  @java.lang.Override()
  public VElem get(java.lang.Object);//  get(java.lang.Object)

  @java.lang.Override()
  public VElem remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  @java.lang.Override()
  public boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public final java.util.Collection<VElem> values();//  values()

  @java.lang.Override()
  public final java.util.Set<KElem> keySet();//  keySet()

  @java.lang.Override()
  public final java.util.Set<java.util.Map.Entry<KElem, VElem>> entrySet();//  entrySet()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(java.util.Map<? extends KElem, ? extends VElem>);//  putAll(java.util.Map<? extends KElem, ? extends VElem>)

  public  CMutableMap3();//  .ctor()
}

public abstract interface IMutableMap /* test.IMutableMap*/<KElem, VElem>  extends java.util.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap {
}
