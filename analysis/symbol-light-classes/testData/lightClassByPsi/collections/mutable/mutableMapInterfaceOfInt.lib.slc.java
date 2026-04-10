public abstract class CMutableMap /* test.CMutableMap*/ implements test.IMutableMap {
  public  CMutableMap();//  .ctor()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/ implements test.IMutableMap {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer> keys;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>> entries;

  private final int size;

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Integer put(int, int);//  put(int, int)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Integer remove(int);//  remove(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Integer get(int);//  get(int)

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMap);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMap)

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public int getSize();//  getSize()
}

public class CMutableMap3 /* test.CMutableMap3*/ implements test.IMutableMap {
  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Integer put(int, int);//  put(int, int)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Integer remove(int);//  remove(int)

  public  CMutableMap3();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public int getSize();//  getSize()
}

public abstract interface IMutableMap /* test.IMutableMap*/ extends java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.Integer, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableMap {
}
