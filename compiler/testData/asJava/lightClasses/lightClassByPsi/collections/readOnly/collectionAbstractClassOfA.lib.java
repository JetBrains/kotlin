public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ACollection /* test.ACollection*/ implements java.util.Collection<test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ACollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(test.A);//  contains(test.A)

  public abstract int getSize();//  getSize()

  public boolean add(test.A);//  add(test.A)

  public boolean addAll(java.util.Collection<? extends test.A>);//  addAll(java.util.Collection<? extends test.A>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super test.A>);//  removeIf(java.util.function.Predicate<? super test.A>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<test.A> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class ACollection2 /* test.ACollection2*/ implements java.util.Collection<test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<test.A> iterator();//  iterator()

  public  ACollection2();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(test.A);//  add(test.A)

  public boolean addAll(java.util.Collection<? extends test.A>);//  addAll(java.util.Collection<? extends test.A>)

  public boolean contains(@org.jetbrains.annotations.NotNull() test.A);//  contains(test.A)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super test.A>);//  removeIf(java.util.function.Predicate<? super test.A>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}
