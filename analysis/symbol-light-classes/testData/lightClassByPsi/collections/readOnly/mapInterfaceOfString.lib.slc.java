public abstract class CMap /* test.CMap*/ implements test.IMap {
  public  CMap();//  .ctor()
}

public abstract class CMap2 /* test.CMap2*/ implements test.IMap {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keys;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> entries;

  private final int size;

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMap);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMap)

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getSize();//  getSize()
}

public class CMap3 /* test.CMap3*/ implements test.IMap {
  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getSize();//  getSize()
}

public abstract interface IMap /* test.IMap*/ extends java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.Map<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
