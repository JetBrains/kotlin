public abstract class CMapEntry /* test.CMapEntry*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
  public  CMapEntry();//  .ctor()

  public VElem setValue(VElem);//  setValue(VElem)
}

public abstract class CMapEntry2 /* test.CMapEntry2*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
  public  CMapEntry2(@org.jetbrains.annotations.NotNull() test.IMapEntry<KElem, VElem>);//  .ctor(test.IMapEntry<KElem, VElem>)

  public KElem getKey();//  getKey()

  public VElem getValue();//  getValue()

  public VElem setValue(VElem);//  setValue(VElem)
}

public class CMapEntry3 /* test.CMapEntry3*/<KElem, VElem>  implements test.IMapEntry<KElem, VElem> {
  public  CMapEntry3();//  .ctor()

  public KElem getKey();//  getKey()

  public VElem getValue();//  getValue()

  public VElem setValue(VElem);//  setValue(VElem)
}

public abstract interface IMapEntry /* test.IMapEntry*/<KElem, VElem>  extends java.util.Map.Entry<KElem, VElem>, kotlin.jvm.internal.markers.KMappedMarker {
}
