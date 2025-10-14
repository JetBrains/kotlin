public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMap /* test.ABMap*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ABMap();//  .ctor()
}

public abstract class ABMap2 /* test.ABMap2*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.B> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>> entries;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> keys;

  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyMap<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B> asJsReadonlyMapView();//  asJsReadonlyMapView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  get(@org.jetbrains.annotations.NotNull() test.A)

  public  ABMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  containsValue(@org.jetbrains.annotations.NotNull() test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class ABMap3 /* test.ABMap3*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  get(@org.jetbrains.annotations.NotNull() test.A)

  public  ABMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  containsValue(@org.jetbrains.annotations.NotNull() test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
