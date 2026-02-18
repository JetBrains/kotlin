public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()
}

public abstract class CList2 /* test.CList2*/ implements test.IList {
  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyArray<@org.jetbrains.annotations.NotNull() java.lang.String> asJsReadonlyArrayView();//  asJsReadonlyArrayView()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CList2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IList);//  .ctor(@org.jetbrains.annotations.NotNull() test.IList)
}

public class CList3 /* test.CList3*/ implements test.IList {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  CList3();//  .ctor()
}

public abstract interface IList /* test.IList*/ extends java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
