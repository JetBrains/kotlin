public abstract class CMutableMapEntry /* test.CMutableMapEntry*/ implements test.IMutableMapEntry {
  public  CMutableMapEntry();//  .ctor()
}

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/ implements test.IMutableMapEntry {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer setValue(int);//  setValue(int)

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getValue();//  getValue()

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry);//  .ctor(test.IMutableMapEntry)
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/ implements test.IMutableMapEntry {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer setValue(int);//  setValue(int)

  public  CMutableMapEntry3();//  .ctor()
}

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/ extends java.util.Map.Entry<java.lang.Integer, java.lang.Integer>, kotlin.jvm.internal.markers.KMutableMap.Entry {
}
