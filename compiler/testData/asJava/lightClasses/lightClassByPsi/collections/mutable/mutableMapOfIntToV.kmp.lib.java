public abstract class SMutableMap /* test.SMutableMap*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  public  SMutableMap();//  .ctor()
}

public abstract class SMutableMap2 /* test.SMutableMap2*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer> keys;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem>> entries;

  private final int size;

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(int, VElem);//  put(int, VElem)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(int);//  remove(int)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsMap<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem> asJsMapView();//  asJsMapView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(int);//  get(int)

  public  SMutableMap2();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, ? extends VElem>)
}

public class SMutableMap3 /* test.SMutableMap3*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(int);//  get(int)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(int, VElem);//  put(int, VElem)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(int);//  remove(int)

  public  SMutableMap3();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, ? extends VElem>)
}
