public abstract class CMutableMap /* test.CMutableMap*/ implements test.IMutableMap {
  public  CMutableMap();//  .ctor()

  public abstract boolean containsKey(java.lang.String);//  containsKey(java.lang.String)

  public abstract boolean containsValue(java.lang.String);//  containsValue(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract java.lang.String get(java.lang.String);//  get(java.lang.String)

  public abstract java.lang.String remove(java.lang.String);//  remove(java.lang.String)

  public abstract java.util.Collection<java.lang.String> getValues();//  getValues()

  public abstract java.util.Set<java.lang.String> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/ implements test.IMutableMap {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public java.lang.String put(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  put(java.lang.String, java.lang.String)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public java.lang.String remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.String get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() test.IMutableMap);//  .ctor(test.IMutableMap)

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(java.lang.String)

  public boolean isEmpty();//  isEmpty()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends java.lang.String, ? extends java.lang.String>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>)
}

public class CMutableMap3 /* test.CMutableMap3*/ implements test.IMutableMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.String> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.String get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public java.lang.String put(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String);//  put(java.lang.String, java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public java.lang.String remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  public  CMutableMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsValue(java.lang.String)

  public boolean isEmpty();//  isEmpty()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.String get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.String remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.String> values();//  values()

  public final java.util.Set<java.lang.String> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.String, java.lang.String>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends java.lang.String, ? extends java.lang.String>);//  putAll(java.util.Map<? extends java.lang.String, ? extends java.lang.String>)
}

public abstract interface IMutableMap /* test.IMutableMap*/ extends java.util.Map<java.lang.String, java.lang.String>, kotlin.jvm.internal.markers.KMutableMap {
}
