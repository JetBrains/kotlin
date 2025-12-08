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

public abstract class Foo /* test.Foo*/ {
  public  Foo();//  .ctor()
}

public abstract interface ICollection /* test.ICollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
