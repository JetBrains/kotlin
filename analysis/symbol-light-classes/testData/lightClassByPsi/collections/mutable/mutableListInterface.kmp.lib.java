public abstract class CMutableList /* test.CMutableList*/<Elem>  implements test.IMutableList<Elem> {
  public  CMutableList();//  .ctor()
}

public abstract class CMutableList2 /* test.CMutableList2*/<Elem>  implements test.IMutableList<Elem> {
  private final int size;

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public Elem removeAt(int);//  removeAt(int)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public Elem set(int, Elem);//  set(int, Elem)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean remove(Elem);//  remove(Elem)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsArray<Elem> asJsArrayView();//  asJsArrayView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyArray<Elem> asJsReadonlyArrayView();//  asJsReadonlyArrayView()

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

  @kotlin.js.JsExport.Ignore()
  public void add(int, Elem);//  add(int, Elem)

  @kotlin.js.JsExport.Ignore()
  public void clear();//  clear()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CMutableList2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableList<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableList<Elem>)

  public int getSize();//  getSize()
}

public class CMutableList3 /* test.CMutableList3*/<Elem>  implements test.IMutableList<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<Elem> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  public  CMutableList3();//  .ctor()

  public Elem get(int);//  get(int)

  public Elem removeAt(int);//  removeAt(int)

  public Elem set(int, Elem);//  set(int, Elem)

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean addAll(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(Elem);//  remove(Elem)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public int getSize();//  getSize()

  public int indexOf(Elem);//  indexOf(Elem)

  public int lastIndexOf(Elem);//  lastIndexOf(Elem)

  public void add(int, Elem);//  add(int, Elem)

  public void clear();//  clear()
}

public abstract interface IMutableList /* test.IMutableList*/<Elem>  extends java.util.List<Elem>, kotlin.jvm.internal.markers.KMutableList {
}
