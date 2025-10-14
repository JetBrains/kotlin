public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class TAMap /* test.TAMap*/<T>  implements java.util.Map<T, @org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  public  TAMap();//  .ctor()
}

public abstract class TAMap2 /* test.TAMap2*/<T>  implements java.util.Map<T, @org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<T, @org.jetbrains.annotations.NotNull() test.A>> entries;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<T> keys;

  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<T, @org.jetbrains.annotations.NotNull() test.A> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<T, @org.jetbrains.annotations.NotNull() test.A>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<T> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.A get(T);//  get(T)

  public  TAMap2();//  .ctor()

  public boolean containsKey(T);//  containsKey(T)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsValue(@org.jetbrains.annotations.NotNull() test.A)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class TAMap3 /* test.TAMap3*/<T>  implements java.util.Map<T, @org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<T, @org.jetbrains.annotations.NotNull() test.A>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<T> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.A get(T);//  get(T)

  public  TAMap3();//  .ctor()

  public boolean containsKey(T);//  containsKey(T)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsValue(@org.jetbrains.annotations.NotNull() test.A)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}
