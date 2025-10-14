public abstract class CMutableMap /* test.CMutableMap*/<KElem, VElem>  implements java.util.Map<KElem, VElem>, kotlin.collections.MutableMap<KElem, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public VElem put(KElem, VElem);//  put(KElem, VElem)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public VElem remove();//  remove()

  @org.jetbrains.annotations.NotNull()
  public java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<KElem> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public java.util.Set<java.util.Map.Entry<KElem, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public VElem get();//  get()

  public  CMutableMap();//  .ctor()

  public boolean containsKey(KElem);//  containsKey(KElem)

  public boolean containsValue(VElem);//  containsValue(VElem)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends KElem, ? extends VElem>);//  putAll(java.util.Map<? extends KElem, ? extends VElem>)
}
