public abstract class CMutableList /* test.CMutableList*/ implements test.IMutableList {
  public  CMutableList();//  .ctor()
}

public abstract class CMutableList2 /* test.CMutableList2*/ implements test.IMutableList {
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String removeAt(int);//  removeAt(int)

  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String set(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  set(int, java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(int, java.util.Collection<? extends java.lang.String>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll();//  removeAll()

  @kotlin.IgnorableReturnValue()
  public boolean retainAll();//  retainAll()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator(int);//  listIterator(int)

  public  CMutableList2(@org.jetbrains.annotations.NotNull() test.IMutableList);//  .ctor(test.IMutableList)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(java.lang.String)

  public void add(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  add(int, java.lang.String)

  public void clear();//  clear()
}

public class CMutableList3 /* test.CMutableList3*/ implements test.IMutableList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String removeAt(int);//  removeAt(int)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String set(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  set(int, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator(int);//  listIterator(int)

  public  CMutableList3();//  .ctor()

  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(java.lang.String)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(int, java.util.Collection<? extends java.lang.String>)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  removeAll(java.util.Collection)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  retainAll(java.util.Collection)

  public int getSize();//  getSize()

  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(java.lang.String)

  public void add(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  add(int, java.lang.String)

  public void clear();//  clear()
}

public abstract interface IMutableList /* test.IMutableList*/ extends java.util.List<java.lang.String>, kotlin.collections.MutableList<java.lang.String>, kotlin.jvm.internal.markers.KMutableList {
}
