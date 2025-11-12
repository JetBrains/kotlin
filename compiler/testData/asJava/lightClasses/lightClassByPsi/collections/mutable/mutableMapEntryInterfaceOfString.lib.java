public abstract class CMutableMapEntry /* test.CMutableMapEntry*/ implements test.IMutableMapEntry {
  public  CMutableMapEntry();//  .ctor()
}

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/ implements test.IMutableMapEntry {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String setValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  setValue(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String getValue();//  getValue()

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry);//  .ctor(test.IMutableMapEntry)
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/ implements test.IMutableMapEntry {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String setValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  setValue(java.lang.String)

  public  CMutableMapEntry3();//  .ctor()
}

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/ extends java.util.Map.Entry<java.lang.String, java.lang.String>, kotlin.jvm.internal.markers.KMutableMap.Entry {
}
