public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(java.lang.String);//  contains(java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract int indexOf(java.lang.String);//  indexOf(java.lang.String)

  public abstract int lastIndexOf(java.lang.String);//  lastIndexOf(java.lang.String)

  public boolean add(java.lang.String);//  add(java.lang.String)

  public boolean addAll(int, java.util.Collection<? extends java.lang.String>);//  addAll(int, java.util.Collection<? extends java.lang.String>)

  public boolean addAll(java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.lang.String remove(int);//  remove(int)

  public java.lang.String set(int, java.lang.String);//  set(int, java.lang.String)

  public java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

  public java.util.ListIterator<java.lang.String> listIterator();//  listIterator()

  public java.util.ListIterator<java.lang.String> listIterator(int);//  listIterator(int)

  public void add(int, java.lang.String);//  add(int, java.lang.String)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<java.lang.String>);//  replaceAll(java.util.function.UnaryOperator<java.lang.String>)

  public void sort(java.util.Comparator<? super java.lang.String>);//  sort(java.util.Comparator<? super java.lang.String>)
}

public abstract class CList2 /* test.CList2*/ implements test.IList {
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

  public  CList2(@org.jetbrains.annotations.NotNull() test.IList);//  .ctor(test.IList)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(java.lang.String);//  add(java.lang.String)

  public boolean addAll(int, java.util.Collection<? extends java.lang.String>);//  addAll(int, java.util.Collection<? extends java.lang.String>)

  public boolean addAll(java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(java.lang.String)

  public java.lang.Object[] toArray();//  toArray()

  public java.lang.String remove(int);//  remove(int)

  public java.lang.String set(int, java.lang.String);//  set(int, java.lang.String)

  public void add(int, java.lang.String);//  add(int, java.lang.String)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<java.lang.String>);//  replaceAll(java.util.function.UnaryOperator<java.lang.String>)

  public void sort(java.util.Comparator<? super java.lang.String>);//  sort(java.util.Comparator<? super java.lang.String>)
}

public class CList3 /* test.CList3*/ implements test.IList {
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

  public  CList3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(java.lang.String);//  add(java.lang.String)

  public boolean addAll(int, java.util.Collection<? extends java.lang.String>);//  addAll(int, java.util.Collection<? extends java.lang.String>)

  public boolean addAll(java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(java.lang.String)

  public java.lang.Object[] toArray();//  toArray()

  public java.lang.String remove(int);//  remove(int)

  public java.lang.String set(int, java.lang.String);//  set(int, java.lang.String)

  public void add(int, java.lang.String);//  add(int, java.lang.String)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<java.lang.String>);//  replaceAll(java.util.function.UnaryOperator<java.lang.String>)

  public void sort(java.util.Comparator<? super java.lang.String>);//  sort(java.util.Comparator<? super java.lang.String>)
}

public abstract interface IList /* test.IList*/ extends java.util.List<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
