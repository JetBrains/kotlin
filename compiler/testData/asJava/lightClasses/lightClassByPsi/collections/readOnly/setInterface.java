public abstract class CSet /* test.CSet*/<Elem>  implements test.ISet<Elem> {
  public  CSet();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public abstract int getSize();//  getSize()

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract class CSet2 /* test.CSet2*/<Elem>  implements test.ISet<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ISet<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.ISet<Elem>)

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public class CSet3 /* test.CSet3*/<Elem>  implements test.ISet<Elem> {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  public  CSet3();//  .ctor()

  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean contains(java.lang.Object);//  contains(java.lang.Object)

  public boolean containsAll(java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  public final int size();//  size()

  public java.lang.Object[] toArray();//  toArray()

  public void clear();//  clear()
}

public abstract interface ISet /* test.ISet*/<Elem>  extends java.util.Set<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
