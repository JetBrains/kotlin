public abstract class CMutableSet /* test.CMutableSet*/<Elem>  implements test.IMutableSet<Elem> {
  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/<Elem>  implements test.IMutableSet<Elem> {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean remove(Elem);//  remove(Elem)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @kotlin.js.JsExport.Ignore()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlySet<Elem> asJsReadonlySetView();//  asJsReadonlySetView()

  @java.lang.Override()
  @kotlin.SinceKotlin(version = @kotlin.SinceKotlin)
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @kotlin.js.JsDontExportDefaultImplementation()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsSet<Elem> asJsSetView();//  asJsSetView()

  @java.lang.Override()
  @kotlin.js.JsExport.Ignore()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  @kotlin.js.JsExport.Ignore()
  public boolean contains(Elem);//  contains(Elem)

  @java.lang.Override()
  @kotlin.js.JsExport.Ignore()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  @kotlin.js.JsExport.Ignore()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  @kotlin.js.JsExport.Ignore()
  public void clear();//  clear()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>)
}

public class CMutableSet3 /* test.CMutableSet3*/<Elem>  implements test.IMutableSet<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean contains(Elem);//  contains(Elem)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean remove(Elem);//  remove(Elem)

  @java.lang.Override()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableSet3();//  .ctor()
}

public abstract interface IMutableSet /* test.IMutableSet*/<Elem>  extends java.util.Set<Elem>, kotlin.jvm.internal.markers.KMutableSet {
}
