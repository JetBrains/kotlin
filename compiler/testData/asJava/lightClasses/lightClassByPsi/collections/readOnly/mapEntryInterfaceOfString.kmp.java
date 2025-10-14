public abstract class CMapEntry /* test.CMapEntry*/ implements test.IMapEntry {
  public  CMapEntry();//  .ctor()
}

public abstract class CMapEntry2 /* test.CMapEntry2*/ implements test.IMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMapEntry);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMapEntry)
}

public class CMapEntry3 /* test.CMapEntry3*/ implements test.IMapEntry {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getKey();//  getKey()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public  CMapEntry3();//  .ctor()
}

public abstract interface IMapEntry /* test.IMapEntry*/ extends java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
