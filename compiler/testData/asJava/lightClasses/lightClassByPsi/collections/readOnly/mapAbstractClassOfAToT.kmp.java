public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ATMap /* test.ATMap*/<T>  implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ATMap();//  .ctor()
}

public abstract class ATMap2 /* test.ATMap2*/<T>  implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<@org.jetbrains.annotations.NotNull() test.A, T> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<T> getValues();//  getValues()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, T>> getEntries();//  getEntries()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> getKeys();//  getKeys()

  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() T get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  get(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  public boolean containsValue(T);//  containsValue(T)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  ATMap2();//  .ctor()
}
