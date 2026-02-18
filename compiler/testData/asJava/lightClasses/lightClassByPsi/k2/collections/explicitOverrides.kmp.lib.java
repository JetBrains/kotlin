public abstract class CCollection /* test.CCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.collections.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  public  CCollection();//  .ctor()

  public boolean contains(Elem);//  contains(Elem)
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  public  CCollection2();//  .ctor()

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public int getSize();//  getSize()
}

public abstract class CCollection3 /* test.CCollection3*/<Elem>  extends test.CCollection2<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public boolean isEmpty();//  isEmpty()
}
