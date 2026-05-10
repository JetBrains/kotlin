public abstract class CMapEntry /* test.CMapEntry*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
  public  CMapEntry();//  .ctor()
}

public abstract class CMapEntry2 /* test.CMapEntry2*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
  @java.lang.Override()
  public KElem getKey();//  getKey()

  @java.lang.Override()
  public VElem getValue();//  getValue()

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMapEntry<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMapEntry<KElem, VElem>)
}

public class CMapEntry3 /* test.CMapEntry3*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
  @java.lang.Override()
  public KElem getKey();//  getKey()

  @java.lang.Override()
  public VElem getValue();//  getValue()

  public  CMapEntry3();//  .ctor()
}

public abstract interface IMapEntry /* test.IMapEntry*/<KElem, VElem>  extends java.util.Map.Entry<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
}
