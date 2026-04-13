public abstract class CMapEntry /* test.CMapEntry*/ implements test.IMapEntry {
  public  CMapEntry();//  .ctor()

  public java.lang.Integer setValue(int);//  setValue(int)
}

public abstract class CMapEntry2 /* test.CMapEntry2*/ implements test.IMapEntry {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getValue();//  getValue()

  public  CMapEntry2(@org.jetbrains.annotations.NotNull() test.IMapEntry);//  .ctor(test.IMapEntry)

  public java.lang.Integer setValue(int);//  setValue(int)
}

public class CMapEntry3 /* test.CMapEntry3*/ implements test.IMapEntry {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getKey();//  getKey()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getValue();//  getValue()

  public  CMapEntry3();//  .ctor()

  public java.lang.Integer setValue(int);//  setValue(int)
}

public abstract interface IMapEntry /* test.IMapEntry*/ extends java.util.Map.Entry<java.lang.Integer, java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
