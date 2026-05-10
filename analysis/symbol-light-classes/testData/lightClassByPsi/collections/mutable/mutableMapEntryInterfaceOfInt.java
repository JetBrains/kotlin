public abstract class CMutableMapEntry /* test.CMutableMapEntry*/ implements test.IMutableMapEntry {
  public  CMutableMapEntry();//  .ctor()
}

public abstract class CMutableMapEntry2 /* test.CMutableMapEntry2*/ implements test.IMutableMapEntry {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  public  CMutableMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMapEntry);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMapEntry)
}

public class CMutableMapEntry3 /* test.CMutableMapEntry3*/ implements test.IMutableMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  public  CMutableMapEntry3();//  .ctor()
}

public abstract interface IMutableMapEntry /* test.IMutableMapEntry*/ extends java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableMap$Entry {
}
