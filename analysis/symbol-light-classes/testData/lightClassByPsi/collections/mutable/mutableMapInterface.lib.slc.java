public abstract class CMutableMap /* test.CMutableMap*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
  public  CMutableMap();//  .ctor()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<KElem, VElem>> entries;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<KElem> keys;

  private final int size;

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(KElem, VElem);//  put(KElem, VElem)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(KElem);//  remove(KElem)

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  @java.lang.Override()
  public boolean containsKey(KElem);//  containsKey(KElem)

  @java.lang.Override()
  public boolean containsValue(VElem);//  containsValue(VElem)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMap<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMap<KElem, VElem>)

  public int getSize();//  getSize()
}

public class CMutableMap3 /* test.CMutableMap3*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(KElem, VElem);//  put(KElem, VElem)

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(KElem);//  remove(KElem)

  @java.lang.Override()
  public boolean containsKey(KElem);//  containsKey(KElem)

  @java.lang.Override()
  public boolean containsValue(VElem);//  containsValue(VElem)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  public  CMutableMap3();//  .ctor()

  public int getSize();//  getSize()
}

public abstract interface IMutableMap /* test.IMutableMap*/<KElem, VElem>  extends java.util.Map<KElem, VElem>, kotlin.collections.MutableMap<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap {
}
