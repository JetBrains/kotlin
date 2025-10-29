public abstract class SMutableSet /* test.SMutableSet*/ implements java.util.Set<java.lang.String>, kotlin.jvm.internal.markers.KMutableSet {
  public  SMutableSet();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(java.lang.String);//  contains(java.lang.String)

  public abstract boolean remove(java.lang.String);//  remove(java.lang.String)

  public abstract int getSize();//  getSize()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class SMutableSet2 /* test.SMutableSet2*/ implements java.util.Set<java.lang.String>, kotlin.jvm.internal.markers.KMutableSet {
  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  SMutableSet2();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}
