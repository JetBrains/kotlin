public abstract class CSet /* test.CSet*/<Elem>  implements test.ISet<Elem> {
  public  CSet();//  .ctor()
}

public abstract class CSet2 /* test.CSet2*/<Elem>  implements test.ISet<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CSet2(@org.jetbrains.annotations.NotNull() test.ISet<Elem>);//  .ctor(test.ISet<Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CSet3 /* test.CSet3*/<Elem>  implements test.ISet<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CSet3();//  .ctor()

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface ISet /* test.ISet*/<Elem>  extends java.util.Set<Elem>, kotlin.collections.Set<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
