public abstract class CSet /* test.CSet*/ implements test.ISet {
  public  CSet();//  .ctor()
}

public abstract class CSet2 /* test.CSet2*/ implements test.ISet {
  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlySet<@org.jetbrains.annotations.NotNull() java.lang.String> asJsReadonlySetView();//  asJsReadonlySetView()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ISet);//  .ctor(@org.jetbrains.annotations.NotNull() test.ISet)
}

public class CSet3 /* test.CSet3*/ implements test.ISet {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet3();//  .ctor()
}

public abstract interface ISet /* test.ISet*/ extends java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.Set<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
