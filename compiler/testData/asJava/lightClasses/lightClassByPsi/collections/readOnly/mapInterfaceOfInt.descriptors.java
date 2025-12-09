public abstract class CMap /* test.CMap*/ implements test.IMap {
  public  CMap();//  .ctor()
}

public abstract class CMap2 /* test.CMap2*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer get(int);//  get(int)

  public  CMap2(@org.jetbrains.annotations.NotNull() test.IMap);//  .ctor(test.IMap)

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CMap3 /* test.CMap3*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer get(int);//  get(int)

  public  CMap3();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface IMap /* test.IMap*/ extends java.util.Map<java.lang.Integer, java.lang.Integer>, kotlin.collections.Map<java.lang.Integer, java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
