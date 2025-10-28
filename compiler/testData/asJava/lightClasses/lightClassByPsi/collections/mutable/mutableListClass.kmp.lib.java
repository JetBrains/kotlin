public abstract class CMutableList /* test.CMutableList*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMutableList {
  public  CMutableList();//  .ctor()
}

public abstract class CMutableList2 /* test.CMutableList2*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMutableList {
  private final int size;

  @kotlin.IgnorableReturnValue()
  public Elem removeAt(int);//  removeAt(int)

  @kotlin.IgnorableReturnValue()
  public Elem set(int, Elem);//  set(int, Elem)

  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(Elem);//  remove(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsArray<Elem> asJsArrayView();//  asJsArrayView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlyArray<Elem> asJsReadonlyArrayView();//  asJsReadonlyArrayView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<Elem> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  public  CMutableList2();//  .ctor()

  public Elem get(int);//  get(int)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(Elem);//  indexOf(Elem)

  public int lastIndexOf(Elem);//  lastIndexOf(Elem)

  public void add(int, Elem);//  add(int, Elem)

  public void clear();//  clear()
}
