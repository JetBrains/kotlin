public abstract class CCollection /* test.CCollection*/ implements test.IMutableCollection {
  public  CCollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(int);//  contains(int)

  public abstract boolean remove(java.lang.Integer);//  remove(java.lang.Integer)

  public abstract int getSize();//  getSize()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CCollection2 /* test.CCollection2*/ implements test.IMutableCollection {
  @kotlin.IgnorableReturnValue()
  public boolean add(int);//  add(int)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  remove(java.lang.Integer)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CCollection2(@org.jetbrains.annotations.NotNull() test.IMutableCollection);//  .ctor(test.IMutableCollection)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public class CCollection3 /* test.CCollection3*/ implements test.IMutableCollection {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(int);//  add(int)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  remove(java.lang.Integer)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract interface IMutableCollection /* test.IMutableCollection*/ extends java.util.Collection<java.lang.Integer>, kotlin.jvm.internal.markers.KMutableCollection {
}
