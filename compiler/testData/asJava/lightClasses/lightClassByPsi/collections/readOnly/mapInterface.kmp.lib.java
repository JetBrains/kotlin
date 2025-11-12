public abstract class CMap /* test.CMap*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  public  CMap();//  .ctor()
}

public abstract class CMap2 /* test.CMap2*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<KElem, VElem>> entries;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<KElem> keys;

  private final int size;

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

  public  CMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMap<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMap<KElem, VElem>)

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CMap3 /* test.CMap3*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  public  CMap3();//  .ctor()

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface IMap /* test.IMap*/<KElem, VElem>  extends java.util.Map<KElem, VElem>, kotlin.collections.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
}
