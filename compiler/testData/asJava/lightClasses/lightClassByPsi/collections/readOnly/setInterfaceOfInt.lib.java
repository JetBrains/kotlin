public abstract class CSet /* test.CSet*/ implements test.ISet {
  public  CSet();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(int);//  contains(int)

  public abstract int getSize();//  getSize()

  public boolean add(int);//  add(int)

  public boolean addAll(java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class CSet2 /* test.CSet2*/ implements test.ISet {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CSet2(@org.jetbrains.annotations.NotNull() test.ISet);//  .ctor(test.ISet)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(int);//  add(int)

  public boolean addAll(java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public class CSet3 /* test.CSet3*/ implements test.ISet {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CSet3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(int);//  add(int)

  public boolean addAll(java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract interface ISet /* test.ISet*/ extends java.util.Set<java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
