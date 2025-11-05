public abstract class CMutableMapEntry /* test.CMutableMapEntry*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  public  CMutableMapEntry();//  .ctor()
}

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public VElem setValue(VElem);//  setValue(VElem)

  @java.lang.Override()
  public KElem getKey();//  getKey()

  @java.lang.Override()
  public VElem getValue();//  getValue()

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMapEntry<KElem, VElem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry<KElem, VElem>)
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/<KElem, VElem>  implements test.IMutableMapEntry<KElem, VElem> {
  @java.lang.Override()
  public KElem getKey();//  getKey()

  @java.lang.Override()
  public VElem getValue();//  getValue()

  @java.lang.Override()
  public VElem setValue(VElem);//  setValue(VElem)

  public  CMutableMapEntry3();//  .ctor()
}

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/<KElem, VElem>  extends java.util.Map.Entry<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap$Entry {
}
