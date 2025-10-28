public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMutableMap /* test.ABMutableMap*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMutableMap {
  public  ABMutableMap();//  .ctor()
}

public abstract class ABMutableMap2 /* test.ABMutableMap2*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMutableMap {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  put(@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  remove(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsMap<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B> asJsMapView();//  asJsMapView()

  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.B> getValues();//  getValues()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>> getEntries();//  getEntries()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> getKeys();//  getKeys()

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  get(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  containsValue(@org.jetbrains.annotations.NotNull() test.B)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  @java.lang.Override()
  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>)

  public  ABMutableMap2();//  .ctor()
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
