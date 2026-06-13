public abstract class CMutableList /* test.CMutableList*/ implements test.IMutableList {
  public  CMutableList();//  .ctor()
}

public abstract class CMutableList2 /* test.CMutableList2*/ implements test.IMutableList {
  private final int size;

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer removeAt(int);//  removeAt(int)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer set(int, int);//  set(int, int)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean add(int);//  add(int)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean remove(int);//  remove(int)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsArray<@org.jetbrains.annotations.NotNull() java.lang.Integer> asJsArrayView();//  asJsArrayView()

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

  @kotlin.js.JsExport.Ignore()
  public void add(int, int);//  add(int, int)

  @kotlin.js.JsExport.Ignore()
  public void clear();//  clear()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> iterator();//  iterator()

  public  CMutableList2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableList);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableList)

  public int getSize();//  getSize()
}

public class CMutableList3 /* test.CMutableList3*/ implements test.IMutableList {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer removeAt(int);//  removeAt(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer set(int, int);//  set(int, int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> listIterator(int);//  listIterator(int)

  public  CMutableList3();//  .ctor()

  public boolean add(int);//  add(int)

  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public boolean addAll(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(int);//  remove(int)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)

  public void add(int, int);//  add(int, int)

  public void clear();//  clear()
}

public abstract interface IMutableList /* test.IMutableList*/ extends java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableList {
}
