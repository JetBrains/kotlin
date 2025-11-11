public abstract class CCollection /* test.CCollection*/<Elem>  implements test.ICollection<Elem> {
  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void clear();//  clear()

  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  public  CCollection2();//  .ctor()
}

public abstract interface ICollection /* test.ICollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
