public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ASet /* test.ASet*/ implements java.util.Set<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ASet();//  .ctor()
}

public abstract class ASet2 /* test.ASet2*/ implements java.util.Set<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlySet<@org.jetbrains.annotations.NotNull() test.A> asJsReadonlySetView();//  asJsReadonlySetView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() test.A> iterator();//  iterator()

  public  ASet2();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  contains(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}
