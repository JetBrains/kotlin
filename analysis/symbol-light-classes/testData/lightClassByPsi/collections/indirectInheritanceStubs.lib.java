public abstract class CCollection /* test.CCollection*/<Elem>  implements test.ICollection<Elem> {
  public  CCollection();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract int getSize();//  getSize()

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super Elem>);//  removeIf(java.util.function.Predicate<? super Elem>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<Elem> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  public  CCollection2();//  .ctor()
}

public abstract class CCollection3 /* test.CCollection3*/ extends test.Foo implements java.util.Collection<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  public  CCollection3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(java.lang.String);//  contains(java.lang.String)

  public abstract int getSize();//  getSize()

  public boolean add(java.lang.String);//  add(java.lang.String)

  public boolean addAll(java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super java.lang.String>);//  removeIf(java.util.function.Predicate<? super java.lang.String>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class CCollection4 /* test.CCollection4*/ extends test.Foo implements test.ICollection<java.lang.String> {
  public  CCollection4();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(java.lang.String);//  contains(java.lang.String)

  public abstract int getSize();//  getSize()

  public boolean add(java.lang.String);//  add(java.lang.String)

  public boolean addAll(java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super java.lang.String>);//  removeIf(java.util.function.Predicate<? super java.lang.String>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public void clear();//  clear()
}

public abstract class CCollection5 /* test.CCollection5*/ extends test.CCollection4 {
  public  CCollection5();//  .ctor()
}

public final class CCollection6 /* test.CCollection6*/ implements java.util.Collection<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public static final test.CCollection6 INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  private  CCollection6();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(java.lang.String);//  add(java.lang.String)

  public boolean addAll(java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean removeIf(java.util.function.Predicate<? super java.lang.String>);//  removeIf(java.util.function.Predicate<? super java.lang.String>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public int getSize();//  getSize()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract class CIterable /* test.CIterable*/ implements test.IIterable {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CIterable();//  .ctor()
}

public abstract class CIterator /* test.CIterator*/ implements test.IIterator {
  public  CIterator();//  .ctor()

  public void remove();//  remove()
}

public abstract class CList /* test.CList*/ implements test.IList {
  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

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

  public java.util.ListIterator<java.lang.String> listIterator();//  listIterator()

  public java.util.ListIterator<java.lang.String> listIterator(int);//  listIterator(int)

  public void add(int, java.lang.String);//  add(int, java.lang.String)

  public void clear();//  clear()

  public void replaceAll(java.util.function.UnaryOperator<java.lang.String>);//  replaceAll(java.util.function.UnaryOperator<java.lang.String>)

  public void sort(java.util.Comparator<? super java.lang.String>);//  sort(java.util.Comparator<? super java.lang.String>)
}

public abstract class CListIterator /* test.CListIterator*/ implements test.IListIterator {
  public  CListIterator();//  .ctor()

  public boolean hasNext();//  hasNext()

  public java.lang.String next();//  next()

  public void add(java.lang.String);//  add(java.lang.String)

  public void remove();//  remove()

  public void set(java.lang.String);//  set(java.lang.String)
}

public abstract class Foo /* test.Foo*/ {
  public  Foo();//  .ctor()
}

public abstract interface ICollection /* test.ICollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}

public abstract interface IIterable /* test.IIterable*/ extends java.lang.Iterable<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public default java.util.Iterator<java.lang.String> iterator();//  iterator()

  public static final class DefaultImpls /* test.IIterable.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static java.util.Iterator<java.lang.String> iterator(@org.jetbrains.annotations.NotNull() test.IIterable);//  iterator(test.IIterable)
  }
}

public abstract interface IIterator /* test.IIterator*/ extends java.util.Iterator<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  public default void remove();//  remove()

  public static final class DefaultImpls /* test.IIterator.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void remove(@org.jetbrains.annotations.NotNull() test.IIterator);//  remove(test.IIterator)
  }
}

public abstract interface IList /* test.IList*/ extends java.util.List<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public default java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

  public static final class DefaultImpls /* test.IList.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static java.util.List<java.lang.String> subList(@org.jetbrains.annotations.NotNull() test.IList, int, int);//  subList(test.IList, int, int)
  }
}

public abstract interface IListIterator /* test.IListIterator*/ extends java.util.ListIterator<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  public default boolean hasNext();//  hasNext()

  public static final class DefaultImpls /* test.IListIterator.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static boolean hasNext(@org.jetbrains.annotations.NotNull() test.IListIterator);//  hasNext(test.IListIterator)
  }
}
