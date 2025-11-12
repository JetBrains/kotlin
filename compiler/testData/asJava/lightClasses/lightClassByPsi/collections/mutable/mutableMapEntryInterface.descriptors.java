public abstract class CMutableMapEntry /* test.CMutableMapEntry*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  public  CMutableMapEntry();//  .ctor()
}

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  @kotlin.IgnorableReturnValue()
  public VElem setValue(VElem);//  setValue(VElem)

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry<KElem, VElem>);//  .ctor(test.IMutableMapEntry<KElem, VElem>)

  public KElem getKey();//  getKey()

  public VElem getValue();//  getValue()
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  public  CMutableMapEntry3();//  .ctor()

  public KElem getKey();//  getKey()

  public VElem getValue();//  getValue()

  public VElem setValue(VElem);//  setValue(VElem)
}

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/<KElem, VElem>  extends java.util.Map.Entry<KElem, VElem>, kotlin.collections.MutableMap.MutableEntry<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap$Entry {
}
