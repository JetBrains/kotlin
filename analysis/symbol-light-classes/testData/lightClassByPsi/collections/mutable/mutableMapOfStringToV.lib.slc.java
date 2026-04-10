public abstract class SMutableMap /* test.SMutableMap*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  public  SMutableMap();//  .ctor()
}

public abstract class SMutableMap2 /* test.SMutableMap2*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String> keys;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>> entries;

  private final int size;

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, VElem);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, VElem)

  @java.lang.Override()
  public boolean containsValue(VElem);//  containsValue(VElem)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  SMutableMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getSize();//  getSize()
}

public class SMutableMap3 /* test.SMutableMap3*/<VElem>  implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.collections.MutableMap<@org.jetbrains.annotations.NotNull() java.lang.String, VElem>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, VElem);//  put(@org.jetbrains.annotations.NotNull() java.lang.String, VElem)

  @java.lang.Override()
  public boolean containsValue(VElem);//  containsValue(VElem)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() java.lang.String, ? extends VElem>)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<VElem> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.lang.String> getKeys();//  getKeys()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<java.util.Map.Entry<java.lang.String, VElem>> getEntries();//  getEntries()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() VElem remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  SMutableMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  containsKey(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getSize();//  getSize()
}
