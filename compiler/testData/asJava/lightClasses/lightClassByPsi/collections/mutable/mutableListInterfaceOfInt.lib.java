public abstract class CMutableList /* test.CMutableList*/ implements test.IMutableList {
  public  CMutableList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(int);//  contains(int)

  public abstract boolean remove(java.lang.Integer);//  remove(java.lang.Integer)

  public abstract int getSize();//  getSize()

  public abstract int indexOf(int);//  indexOf(int)

  public abstract int lastIndexOf(int);//  lastIndexOf(int)

  public abstract java.lang.Integer removeAt(int);//  removeAt(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int remove(int);//  remove(int)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CMutableList2 /* test.CMutableList2*/ implements test.IMutableList {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer removeAt(int);//  removeAt(int)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer set(int, int);//  set(int, int)

  @kotlin.IgnorableReturnValue()
  public boolean add(int);//  add(int)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(int, java.util.Collection<? extends java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  remove(java.lang.Integer)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.Integer> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator(int);//  listIterator(int)

  public  CMutableList2(@org.jetbrains.annotations.NotNull() test.IMutableList);//  .ctor(test.IMutableList)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int remove(int);//  remove(int)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)

  public java.lang.Object[] toArray();//  toArray()

  public void add(int, int);//  add(int, int)

  public void clear();//  clear()
}

public class CMutableList3 /* test.CMutableList3*/ implements test.IMutableList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer removeAt(int);//  removeAt(int)

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer set(int, int);//  set(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.Integer> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator(int);//  listIterator(int)

  public  CMutableList3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(int);//  add(int)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(int, java.util.Collection<? extends java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(@org.jetbrains.annotations.Nullable() java.lang.Integer);//  remove(java.lang.Integer)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int remove(int);//  remove(int)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)

  public java.lang.Object[] toArray();//  toArray()

  public void add(int, int);//  add(int, int)

  public void clear();//  clear()
}

public abstract interface IMutableList /* test.IMutableList*/ extends java.util.List<java.lang.Integer>, kotlin.jvm.internal.markers.KMutableList {
}
