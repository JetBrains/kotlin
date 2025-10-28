public abstract class CMutableList /* test.CMutableList*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMutableList {
  public  CMutableList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract Elem removeAt(int);//  removeAt(int)

  public abstract int getSize();//  getSize()

  public final Elem remove(int);//  remove(int)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CMutableList2 /* test.CMutableList2*/<Elem>  implements java.util.List<Elem>, kotlin.jvm.internal.markers.KMutableList {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public Elem removeAt(int);//  removeAt(int)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public Elem set(int, Elem);//  set(int, Elem)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean addAll(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(int, @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<Elem> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<Elem> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public Elem get(int);//  get(int)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void add(int, Elem);//  add(int, Elem)

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableList2();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final Elem remove(int);//  remove(int)

  public final int size();//  size()

  public int getSize();//  getSize()

  public int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public java.lang.Object[] toArray();//  toArray()
}
