public abstract class CList /* test.CList*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  public  CList();//  .ctor()
}

public abstract class CList2 /* test.CList2*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyArray<Elem> asJsReadonlyArrayView();//  asJsReadonlyArrayView()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<Elem> subList(int, int);//  subList(int, int)

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator();//  listIterator()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  @kotlin.js.JsExport.Ignore()
  public Elem get(int);//  get(int)

  @kotlin.js.JsExport.Ignore()
  public boolean contains(Elem);//  contains(Elem)

  @kotlin.js.JsExport.Ignore()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.js.JsExport.Ignore()
  public boolean isEmpty();//  isEmpty()

  @kotlin.js.JsExport.Ignore()
  public int indexOf(Elem);//  indexOf(Elem)

  @kotlin.js.JsExport.Ignore()
  public int lastIndexOf(Elem);//  lastIndexOf(Elem)

  public  CList2();//  .ctor()

  public int getSize();//  getSize()
}

public class CList3 /* test.CList3*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<Elem> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  public  CList3();//  .ctor()

  public Elem get(int);//  get(int)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(Elem);//  indexOf(Elem)

  public int lastIndexOf(Elem);//  lastIndexOf(Elem)
}
