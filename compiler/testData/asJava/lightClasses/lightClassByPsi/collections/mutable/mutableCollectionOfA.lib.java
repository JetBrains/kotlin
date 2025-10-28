public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class AMutableCollection /* test.AMutableCollection*/ implements java.util.Collection<test.A>, kotlin.jvm.internal.markers.KMutableCollection {
  public  AMutableCollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(test.A);//  contains(test.A)

  public abstract boolean remove(test.A);//  remove(test.A)

  public abstract int getSize();//  getSize()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class AMutableCollection2 /* test.AMutableCollection2*/ implements java.util.Collection<test.A>, kotlin.jvm.internal.markers.KMutableCollection {
  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() test.A);//  add(test.A)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends test.A>);//  addAll(java.util.Collection<? extends test.A>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() test.A);//  remove(test.A)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<test.A> iterator();//  iterator()

  public  AMutableCollection2();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(@org.jetbrains.annotations.NotNull() test.A);//  contains(test.A)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}
