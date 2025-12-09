public abstract class CMutableMap /* test.CMutableMap*/ implements test.IMutableMap {
  public  CMutableMap();//  .ctor()

  public abstract boolean containsKey(int);//  containsKey(int)

  public abstract boolean containsValue(int);//  containsValue(int)

  public abstract int getSize();//  getSize()

  public abstract java.lang.Integer get(int);//  get(int)

  public abstract java.lang.Integer remove(int);//  remove(int)

  public abstract java.util.Collection<java.lang.Integer> getValues();//  getValues()

  public abstract java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  public abstract java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.Integer> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> entrySet();//  entrySet()
}

public abstract class CMutableMap2 /* test.CMutableMap2*/ implements test.IMutableMap {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer put(int, int);//  put(int, int)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer remove(int);//  remove(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer get(int);//  get(int)

  public  CMutableMap2(@org.jetbrains.annotations.NotNull() test.IMutableMap);//  .ctor(test.IMutableMap)

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean isEmpty();//  isEmpty()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.Integer> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>)
}

public class CMutableMap3 /* test.CMutableMap3*/ implements test.IMutableMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<java.lang.Integer> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.Integer> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer put(int, int);//  put(int, int)

  @org.jetbrains.annotations.Nullable()
  public java.lang.Integer remove(int);//  remove(int)

  public  CMutableMap3();//  .ctor()

  public boolean containsKey(int);//  containsKey(int)

  public boolean containsValue(int);//  containsValue(int)

  public boolean isEmpty();//  isEmpty()

  public final boolean containsKey(java.lang.Object);//  containsKey(java.lang.Object)

  public final boolean containsValue(java.lang.Object);//  containsValue(java.lang.Object)

  public final int size();//  size()

  public final java.lang.Integer get(java.lang.Object);//  get(java.lang.Object)

  public final java.lang.Integer remove(java.lang.Object);//  remove(java.lang.Object)

  public final java.util.Collection<java.lang.Integer> values();//  values()

  public final java.util.Set<java.lang.Integer> keySet();//  keySet()

  public final java.util.Set<java.util.Map.Entry<java.lang.Integer, java.lang.Integer>> entrySet();//  entrySet()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>);//  putAll(java.util.Map<? extends java.lang.Integer, ? extends java.lang.Integer>)
}

public abstract interface IMutableMap /* test.IMutableMap*/ extends java.util.Map<java.lang.Integer, java.lang.Integer>, kotlin.jvm.internal.markers.KMutableMap {
}
