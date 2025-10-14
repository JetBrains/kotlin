public abstract interface ASet /* ASet*/<T>  extends java.util.Collection<T>, kotlin.jvm.internal.markers.KMutableCollection {
}

public final class MyList /* MyList*/ implements java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String get(int);//  get(int)

  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public @org.jetbrains.annotations.NotNull() java.lang.String remove(int);//  remove(int)

  @java.lang.Override()
  public @org.jetbrains.annotations.NotNull() java.lang.String set(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  set(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public abstract int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public abstract int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean addAll(int, java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(int, java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  addAll(java.util.Collection<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final boolean contains(java.lang.Object);//  contains(java.lang.Object)

  @java.lang.Override()
  public final int indexOf(java.lang.Object);//  indexOf(java.lang.Object)

  @java.lang.Override()
  public final int lastIndexOf(java.lang.Object);//  lastIndexOf(java.lang.Object)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void add(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  add(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public void clear();//  clear()

  public  MyList();//  .ctor()
}

public abstract class MySet /* MySet*/<T>  implements ASet<T> {
  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public boolean remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  remove(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  public  MySet();//  .ctor()
}

public abstract class SmartSet /* SmartSet*/<T>  {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<T> iterator();//  iterator()

  @java.lang.Override()
  public boolean add(T);//  add(T)

  private  SmartSet();//  .ctor()
}
