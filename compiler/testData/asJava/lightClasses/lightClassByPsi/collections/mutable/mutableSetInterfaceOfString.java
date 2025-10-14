public abstract class CMutableSet /* test.CMutableSet*/ implements test.IMutableSet {
  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public abstract boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/ implements test.IMutableSet {
  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableSet);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableSet)
}

public class CMutableSet3 /* test.CMutableSet3*/ implements test.IMutableSet {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public boolean add(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void clear();//  clear()

  public  CMutableSet3();//  .ctor()
}

public abstract interface IMutableSet /* test.IMutableSet*/ extends java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableSet {
}
