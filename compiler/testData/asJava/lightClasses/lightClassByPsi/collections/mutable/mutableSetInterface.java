public abstract class CMutableSet /* test.CMutableSet*/<Elem>  implements test.IMutableSet<Elem> {
  public  CMutableSet();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract int getSize();//  getSize()

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/<Elem>  implements test.IMutableSet<Elem> {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()
}

public class CMutableSet3 /* test.CMutableSet3*/<Elem>  implements test.IMutableSet<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableSet3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract interface IMutableSet /* test.IMutableSet*/<Elem>  extends java.util.Set<Elem>, kotlin.jvm.internal.markers.KMutableSet {
}
