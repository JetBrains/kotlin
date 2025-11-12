public abstract class CCollection /* test.CCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  public  CCollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract int getSize();//  getSize()

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super Elem>);//  removeIf(java.util.function.Predicate<? super Elem>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<Elem> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  public  CCollection2();//  .ctor()

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public int getSize();//  getSize()
}

public abstract class CCollection3 /* test.CCollection3*/<Elem>  extends test.CCollection2<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public boolean isEmpty();//  isEmpty()
}
