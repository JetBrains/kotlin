public abstract class CMapEntry /* test.CMapEntry*/ implements test.IMapEntry {
  public  CMapEntry();//  .ctor()
}

public abstract class CMapEntry2 /* test.CMapEntry2*/ implements test.IMapEntry {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getValue();//  getValue()

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() test.IMapEntry);//  .ctor(test.IMapEntry)
}

public class CMapEntry3 /* test.CMapEntry3*/ implements test.IMapEntry {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getValue();//  getValue()

  public  CMapEntry3();//  .ctor()
}

public abstract interface IMapEntry /* test.IMapEntry*/ extends java.util.Map.Entry<java.lang.Integer, java.lang.Integer>, kotlin.collections.Map.Entry<java.lang.Integer, java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
