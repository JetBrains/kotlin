public abstract class SMutableMap /* test.SMutableMap*/<VElem>  implements java.util.Map<java.lang.String, VElem>, kotlin.collections.MutableMap<java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  public  SMutableMap();//  .ctor()
}

public abstract class SMutableMap2 /* test.SMutableMap2*/<VElem>  implements java.util.Map<java.lang.String, VElem>, kotlin.collections.MutableMap<java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public VElem put(@org.jetbrains.annotations.NotNull() java.lang.String, VElem);//  put(java.lang.String, VElem)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public VElem remove();//  remove()

  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get();//  get()

  public  SMutableMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends java.lang.String, ? extends VElem>);//  putAll(java.util.Map<? extends java.lang.String, ? extends VElem>)
}

public class SMutableMap3 /* test.SMutableMap3*/<VElem>  implements java.util.Map<java.lang.String, VElem>, kotlin.collections.MutableMap<java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get(@org.jetbrains.annotations.NotNull() java.lang.String);//  get(java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public VElem put(@org.jetbrains.annotations.NotNull() java.lang.String, VElem);//  put(java.lang.String, VElem)

  @org.jetbrains.annotations.Nullable()
  public VElem remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  public  SMutableMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(java.lang.String)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends java.lang.String, ? extends VElem>);//  putAll(java.util.Map<? extends java.lang.String, ? extends VElem>)
}
