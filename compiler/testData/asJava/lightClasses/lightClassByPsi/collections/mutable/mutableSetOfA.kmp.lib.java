public abstract class AMutableSet /* test.AMutableSet*/ implements java.util.Set<@org.jetbrains.annotations.NotNull() test.A>, kotlin.collections.MutableSet<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMutableSet {
  private final int size;

  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  add(@org.jetbrains.annotations.NotNull() test.A)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  remove(@org.jetbrains.annotations.NotNull() test.A)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>)

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlySet<@org.jetbrains.annotations.NotNull() test.A> asJsReadonlySetView();//  asJsReadonlySetView()

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsSet<@org.jetbrains.annotations.NotNull() test.A> asJsSetView();//  asJsSetView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() test.A> iterator();//  iterator()

  public  AMutableSet();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  contains(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()
}
