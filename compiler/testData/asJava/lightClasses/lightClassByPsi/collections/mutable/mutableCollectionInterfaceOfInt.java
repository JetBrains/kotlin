public abstract class CCollection /* test.CCollection*/ implements test.IMutableCollection {
  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public abstract boolean contains(int);//  contains(int)

  @java.lang.Override()
  public abstract boolean remove(int);//  remove(int)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/ implements test.IMutableCollection {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean add(int);//  add(int)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean remove(int);//  remove(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> iterator();//  iterator()

  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean contains(int);//  contains(int)

  @java.lang.Override()
  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void clear();//  clear()

  public  CCollection2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableCollection);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableCollection)
}

public class CCollection3 /* test.CCollection3*/ implements test.IMutableCollection {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.Integer> iterator();//  iterator()

  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public boolean add(int);//  add(int)

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean contains(int);//  contains(int)

  @java.lang.Override()
  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean remove(int);//  remove(int)

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void clear();//  clear()

  public  CCollection3();//  .ctor()
}

public abstract interface IMutableCollection /* test.IMutableCollection*/ extends java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableCollection {
}
