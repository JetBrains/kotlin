public abstract class CCollection /* test.CCollection*/<Elem>  implements test.ICollection<Elem> {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  implements test.ICollection<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CCollection2(@org.jetbrains.annotations.NotNull() test.ICollection<Elem>);//  .ctor(test.ICollection<Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CCollection3 /* test.CCollection3*/<Elem>  implements test.ICollection<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface ICollection /* test.ICollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.collections.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
