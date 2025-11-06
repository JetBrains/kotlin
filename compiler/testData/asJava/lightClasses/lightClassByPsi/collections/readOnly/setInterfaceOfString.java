public abstract class CSet /* test.CSet*/ implements test.ISet {
  public  CSet();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract int getSize();//  getSize()

  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract class CSet2 /* test.CSet2*/ implements test.ISet {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ISet);//  .ctor(@org.jetbrains.annotations.NotNull() test.ISet)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public class CSet3 /* test.CSet3*/ implements test.ISet {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract interface ISet /* test.ISet*/ extends java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
