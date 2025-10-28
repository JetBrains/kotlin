public abstract class CMutableCollection /* test.CMutableCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMutableCollection {
  public  CMutableCollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract int getSize();//  getSize()

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CMutableCollection2 /* test.CMutableCollection2*/<Elem>  implements java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMutableCollection {
  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CMutableCollection2();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}
