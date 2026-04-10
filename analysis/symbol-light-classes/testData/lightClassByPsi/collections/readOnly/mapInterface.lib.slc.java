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

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  @java.lang.Override()
  public boolean containsKey(KElem);//  containsKey(KElem)

  @java.lang.Override()
  public boolean containsValue(VElem);//  containsValue(VElem)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  public  CMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMap<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMap<KElem, VElem>)

  public int getSize();//  getSize()
}

public class CMap3 /* test.CMap3*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(KElem);//  get(KElem)

  @java.lang.Override()
  public boolean containsKey(KElem);//  containsKey(KElem)

  @java.lang.Override()
  public boolean containsValue(VElem);//  containsValue(VElem)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  public  CMap3();//  .ctor()

  public int getSize();//  getSize()
}

public abstract interface IMap /* test.IMap*/<KElem, VElem>  extends java.util.Map<KElem, VElem>, kotlin.collections.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
}
