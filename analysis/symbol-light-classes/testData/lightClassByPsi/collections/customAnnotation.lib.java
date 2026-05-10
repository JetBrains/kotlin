@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* test.Ann*/ {
  public abstract int x();//  x()
}

public abstract class CCollection /* test.CCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  @test.Ann(x = 1)
  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  @test.Ann(x = 2)
  public final void foo();//  foo()

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
