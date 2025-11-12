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

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(KElem, VElem);//  put(KElem, VElem)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(KElem);//  remove(KElem)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsMap<KElem, VElem> asJsMapView();//  asJsMapView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<KElem, VElem> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMap<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMap<KElem, VElem>)

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>)
}

public class CMutableMap3 /* test.CMutableMap3*/<KElem, VElem>  implements test.IMutableMap<KElem, VElem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(KElem, VElem);//  put(KElem, VElem)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(KElem);//  remove(KElem)

  public  CMutableMap3();//  .ctor()

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>)
}

public abstract interface IMutableMap /* test.IMutableMap*/<KElem, VElem>  extends java.util.Map<KElem, VElem>, kotlin.collections.MutableMap<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap {
}
