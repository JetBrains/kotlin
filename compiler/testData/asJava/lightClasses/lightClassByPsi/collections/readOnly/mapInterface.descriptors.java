public abstract class CMap /* test.CMap*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  public  CMap();//  .ctor()
}

public abstract class CMap2 /* test.CMap2*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get();//  get()

  public  CMap2(@org.jetbrains.annotations.NotNull() test.IMap<KElem, VElem>);//  .ctor(test.IMap<KElem, VElem>)

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CMap3 /* test.CMap3*/<KElem, VElem>  implements test.IMap<KElem, VElem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get(java.lang.Object);//  get(java.lang.Object)

  public  CMap3();//  .ctor()

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface IMap /* test.IMap*/<KElem, VElem>  extends java.util.Map<KElem, VElem>, kotlin.collections.Map<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
}
