public abstract class SMutableMap /* test.SMutableMap*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  public  SMutableMap();//  .ctor()
}

public abstract class SMutableMap2 /* test.SMutableMap2*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keys;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>> entries;

  private final int size;

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, VElem);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, VElem)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem> asJsMapView();//  asJsMapView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  SMutableMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>)
}

public class SMutableMap3 /* test.SMutableMap3*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, VElem);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, VElem)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  SMutableMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>)
}
