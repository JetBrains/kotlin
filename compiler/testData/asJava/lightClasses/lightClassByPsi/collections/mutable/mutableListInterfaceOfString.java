public abstract class CMutableList /* test.CMutableList*/ implements test.IMutableList {
  public  CMutableList();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  public abstract boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract int getSize();//  getSize()

  public abstract int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract class CMutableList2 /* test.CMutableList2*/ implements test.IMutableList {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String set(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  set(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void add(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableList2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableList);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableList)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean addAll(int, java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(int, java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()
}

public class CMutableList3 /* test.CMutableList3*/ implements test.IMutableList {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String removeAt(int);//  removeAt(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String set(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  set(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public boolean add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public int indexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public int lastIndexOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void add(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableList3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean addAll(int, java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(int, java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()
}

public abstract interface IMutableList /* test.IMutableList*/ extends java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableList {
}
