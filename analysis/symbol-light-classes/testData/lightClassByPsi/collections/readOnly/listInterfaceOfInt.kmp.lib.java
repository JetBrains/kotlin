public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()
}

public abstract class CList2 /* test.CList2*/ implements test.IList {
  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyArray<@org.jetbrains.annotations.NotNull() java.lang.Integer> asJsReadonlyArrayView();//  asJsReadonlyArrayView()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer get(int);//  get(int)

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> iterator();//  iterator()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer> subList(int, int);//  subList(int, int)

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> listIterator();//  listIterator()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> listIterator(int);//  listIterator(int)

  @kotlin.js.JsExport.Ignore()
  public boolean contains(int);//  contains(int)

  @kotlin.js.JsExport.Ignore()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.js.JsExport.Ignore()
  public boolean isEmpty();//  isEmpty()

  @kotlin.js.JsExport.Ignore()
  public int indexOf(int);//  indexOf(int)

  @kotlin.js.JsExport.Ignore()
  public int lastIndexOf(int);//  lastIndexOf(int)

  public  CList2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IList);//  .ctor(@org.jetbrains.annotations.NotNull() test.IList)

  public int getSize();//  getSize()
}

public class CList3 /* test.CList3*/ implements test.IList {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> listIterator(int);//  listIterator(int)

  public  CList3();//  .ctor()

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)
}

public abstract interface IList /* test.IList*/ extends java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
