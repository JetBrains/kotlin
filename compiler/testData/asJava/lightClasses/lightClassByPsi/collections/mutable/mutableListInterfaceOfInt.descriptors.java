public abstract class CMutableList /* test.CMutableList*/ implements test.IMutableList {
  public  CMutableList();//  .ctor()
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
  public boolean remove(int);//  remove(int)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll();//  removeAll()

  @kotlin.IgnorableReturnValue()
  public boolean retainAll();//  retainAll()

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

  public boolean contains(int);//  contains(int)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)

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

  public boolean add(int);//  add(int)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(int, java.util.Collection<? extends java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(int);//  remove(int)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  removeAll(java.util.Collection)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  retainAll(java.util.Collection)

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)

  public void add(int, int);//  add(int, int)

  public void clear();//  clear()
}

public abstract interface IMutableList /* test.IMutableList*/ extends java.util.List<java.lang.Integer>, kotlin.collections.MutableList<java.lang.Integer>, kotlin.jvm.internal.markers.KMutableList {
}
