public abstract class CSet /* test.CSet*/<Elem>  implements test.ISet<Elem> {
  public  CSet();//  .ctor()
}

public abstract class CSet2 /* test.CSet2*/<Elem>  implements test.ISet<Elem> {
  private final int size;

  @kotlin.SinceKotlin()
  @kotlin.js.ExperimentalJsCollectionsApi()
  @kotlin.js.ExperimentalJsExport()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() kotlin.js.collections.JsReadonlySet<Elem> asJsReadonlySetView();//  asJsReadonlySetView()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ISet<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.ISet<Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CSet3 /* test.CSet3*/<Elem>  implements test.ISet<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CSet3();//  .ctor()

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface ISet /* test.ISet*/<Elem>  extends java.util.Set<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
