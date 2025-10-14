public abstract class CMutableMap /* test.CMutableMap*/ implements test.IMutableMap {
  public  CMutableMap();//  .ctor()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> getValues();//  getValues()

  public abstract java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> getEntries();//  getEntries()

  public final @org.jetbrains.annotations.NotNull() java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> values();//  values()

  public final java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> entrySet();//  entrySet()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/ implements test.IMutableMap {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> getValues();//  getValues()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> getEntries();//  getEntries()

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableMap);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableMap)

  public final @org.jetbrains.annotations.NotNull() java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> values();//  values()

  public final java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> entrySet();//  entrySet()

  public void putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends @org.jetbrains.annotations.NotNull() java.lang.String>)
}

public class CMutableMap3 /* test.CMutableMap3*/ implements test.IMutableMap {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> getValues();//  getValues()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> getKeys();//  getKeys()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> getEntries();//  getEntries()

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.String remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableMap3();//  .ctor()

  public final @org.jetbrains.annotations.NotNull() java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String> values();//  values()

  public final java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>> entrySet();//  entrySet()

  public void putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  putAll(java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends @org.jetbrains.annotations.NotNull() java.lang.String>)
}

public abstract interface IMutableMap /* test.IMutableMap*/ extends java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
}
