public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ATMap /* test.ATMap*/<T>  implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ATMap();//  .ctor()
}

public abstract class ATMap2 /* test.ATMap2*/<T>  implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, T>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<T> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, T>> entries;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> keys;

  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<@org.jetbrains.annotations.NotNull() test.A, T> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<T> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, T>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() T get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  get(@org.jetbrains.annotations.NotNull() test.A)

  public  ATMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsValue(T);//  containsValue(T)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}
