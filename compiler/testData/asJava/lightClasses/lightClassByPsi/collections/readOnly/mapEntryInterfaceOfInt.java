public abstract class CMapEntry /* test.CMapEntry*/ implements test.IMapEntry {
  @java.lang.Override()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  public  CMapEntry();//  .ctor()
}

public abstract class CMapEntry2 /* test.CMapEntry2*/ implements test.IMapEntry {
  @java.lang.Override()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  @java.lang.Override()
  public int getKey();//  getKey()

  @java.lang.Override()
  public int getValue();//  getValue()

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMapEntry);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMapEntry)
}

public class CMapEntry3 /* test.CMapEntry3*/ implements test.IMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getValue();//  getValue()

  @java.lang.Override()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer setValue(int);//  setValue(int)

  public  CMapEntry3();//  .ctor()
}

public abstract interface IMapEntry /* test.IMapEntry*/ extends java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
