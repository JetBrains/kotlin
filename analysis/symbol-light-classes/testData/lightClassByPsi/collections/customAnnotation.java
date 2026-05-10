@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* test.Ann*/ {
  public abstract int x();//  x()
}

public abstract class CCollection /* test.CCollection*/<Elem>  implements java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @test.Ann(x = 1)
  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<?>);//  containsAll(java.util.Collection<?>)

  @java.lang.Override()
  public <T> T[] toArray(T[]);// <T>  toArray(T[])

  @java.lang.Override()
  public abstract int getSize();//  getSize()

  @java.lang.Override()
  public boolean add(Elem);//  add(Elem)

  @java.lang.Override()
  public boolean addAll(java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  @java.lang.Override()
  public boolean remove(java.lang.Object);//  remove(java.lang.Object)

  @java.lang.Override()
  public boolean removeAll(java.util.Collection<?>);//  removeAll(java.util.Collection<?>)

  @java.lang.Override()
  public boolean retainAll(java.util.Collection<?>);//  retainAll(java.util.Collection<?>)

  @java.lang.Override()
  public final int size();//  size()

  @java.lang.Override()
  public java.lang.Object[] toArray();//  toArray()

  @java.lang.Override()
  public void clear();//  clear()

  @test.Ann(x = 2)
  public final void foo();//  foo()

  public  CCollection();//  .ctor()
}
