public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ABMutableMap /* test.ABMutableMap*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMutableMap {
  public  ABMutableMap();//  .ctor()
}

public abstract class ABMutableMap2 /* test.ABMutableMap2*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.B> values;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>> entries;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> keys;

  private final int size;

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  put(@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  remove(@org.jetbrains.annotations.NotNull() test.A)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsMap<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B> asJsMapView();//  asJsMapView()

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

  public  ABMutableMap2();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  containsValue(@org.jetbrains.annotations.NotNull() test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>)
}

public class ABMutableMap3 /* test.ABMutableMap3*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>, kotlin.jvm.internal.markers.KMutableMap {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.B> getValues();//  getValues()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() java.util.Map.Entry<@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Set<@org.jetbrains.annotations.NotNull() test.A> getKeys();//  getKeys()

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  get(@org.jetbrains.annotations.NotNull() test.A)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B put(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  put(@org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B)

  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() test.B remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  remove(@org.jetbrains.annotations.NotNull() test.A)

  public  ABMutableMap3();//  .ctor()

  public boolean containsKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  containsKey(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsValue(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.B);//  containsValue(@org.jetbrains.annotations.NotNull() test.B)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void putAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>);//  putAll(@org.jetbrains.annotations.NotNull() java.util.Map<? extends @org.jetbrains.annotations.NotNull() test.A, @org.jetbrains.annotations.NotNull() test.B>)
}

public final class B /* test.B*/ {
  public  B();//  .ctor()
}
