public abstract class CMutableSet /* test.CMutableSet*/<Elem>  implements test.IMutableSet<Elem> {
  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/<Elem>  implements test.IMutableSet<Elem> {
  private final int size;

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

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
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlySet<Elem> asJsReadonlySetView();//  asJsReadonlySetView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsSet<Elem> asJsSetView();//  asJsSetView()

  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @kotlin.js.JsExport.Ignore()
  public boolean contains(Elem);//  contains(Elem)

  @kotlin.js.JsExport.Ignore()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.js.JsExport.Ignore()
  public boolean isEmpty();//  isEmpty()

  @kotlin.js.JsExport.Ignore()
  public void clear();//  clear()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>)

  public int getSize();//  getSize()
}

public class CMutableSet3 /* test.CMutableSet3*/<Elem>  implements test.IMutableSet<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CMutableSet3();//  .ctor()

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(Elem);//  remove(Elem)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public abstract interface IMutableSet /* test.IMutableSet*/<Elem>  extends java.util.Set<Elem>, kotlin.jvm.internal.markers.KMutableSet {
}
