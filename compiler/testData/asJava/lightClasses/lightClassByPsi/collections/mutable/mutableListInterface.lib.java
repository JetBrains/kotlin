public abstract class CMutableList /* test.CMutableList*/<Elem>  implements test.IMutableList<Elem> {
  public  CMutableList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract Elem removeAt(int);//  removeAt(int)

  public abstract int getSize();//  getSize()

  public final Elem remove(int);//  remove(int)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CMutableList2 /* test.CMutableList2*/<Elem>  implements test.IMutableList<Elem> {
  @kotlin.IgnorableReturnValue()
  public Elem removeAt(int);//  removeAt(int)

  @kotlin.IgnorableReturnValue()
  public Elem set(int, Elem);//  set(int, Elem)

  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(int, java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<Elem> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<Elem> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  public  CMutableList2(@org.jetbrains.annotations.NotNull() test.IMutableList<Elem>);//  .ctor(test.IMutableList<Elem>)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public Elem get(int);//  get(int)

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public final Elem remove(int);//  remove(int)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public java.lang.Object[] toArray();//  toArray()

  public void add(int, Elem);//  add(int, Elem)

  public void clear();//  clear()
}

public class CMutableList3 /* test.CMutableList3*/<Elem>  implements test.IMutableList<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<Elem> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<Elem> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  public  CMutableList3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public Elem get(int);//  get(int)

  public Elem removeAt(int);//  removeAt(int)

  public Elem set(int, Elem);//  set(int, Elem)

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(int, java.util.Collection<? extends Elem>)

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final Elem remove(int);//  remove(int)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public java.lang.Object[] toArray();//  toArray()

  public void add(int, Elem);//  add(int, Elem)

  public void clear();//  clear()
}

public abstract interface IMutableList /* test.IMutableList*/<Elem>  extends java.util.List<Elem>, kotlin.jvm.internal.markers.KMutableList {
}
