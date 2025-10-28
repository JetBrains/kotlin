public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class AMutableSet /* test.AMutableSet*/ implements java.util.Set<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMutableSet {
  public  AMutableSet();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(@org.jetbrains.annotations.NotNull() test.A);//  contains(@org.jetbrains.annotations.NotNull() test.A)

  public abstract boolean remove(@org.jetbrains.annotations.NotNull() test.A);//  remove(@org.jetbrains.annotations.NotNull() test.A)

  public abstract int getSize();//  getSize()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class AMutableSet2 /* test.AMutableSet2*/ implements java.util.Set<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMutableSet {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  add(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  remove(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() test.A> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  contains(@org.jetbrains.annotations.NotNull() test.A)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  public  AMutableSet2();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() test.A>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() test.A>)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()
}
