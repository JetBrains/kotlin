public abstract class CCollection /* test.CCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.collections.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public boolean contains(Elem);//  contains(Elem)

  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CCollection2();//  .ctor()
}

public abstract class CCollection3 /* test.CCollection3*/<Elem>  extends test.CCollection2<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  public  CCollection3();//  .ctor()
}
