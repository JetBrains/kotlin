public abstract class CMap /* test.CMap*/ implements test.IMap {
  public  CMap();//  .ctor()
}

public abstract class CMap2 /* test.CMap2*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.String get();//  get()

  public  CMap2(@org.jetbrains.annotations.NotNull() test.IMap);//  .ctor(test.IMap)

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(java.lang.String)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CMap3 /* test.CMap3*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.String get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  public  CMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(java.lang.String)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface IMap /* test.IMap*/ extends java.util.Map<java.lang.String, java.lang.String>, kotlin.collections.Map<java.lang.String, java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
