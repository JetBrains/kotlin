public abstract class CSet /* test.CSet*/<Elem>  implements test.ISet<Elem> {
  public  CSet();//  .ctor()
}

public abstract class CSet2 /* test.CSet2*/<Elem>  implements test.ISet<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(Elem);//  contains(Elem)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ISet<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.ISet<Elem>)
}

public class CSet3 /* test.CSet3*/<Elem>  implements test.ISet<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(Elem);//  contains(Elem)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet3();//  .ctor()
}

public abstract interface ISet /* test.ISet*/<Elem>  extends java.util.Set<Elem>, kotlin.collections.Set<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
