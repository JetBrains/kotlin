public abstract class CMutableSet /* test.CMutableSet*/ implements test.IMutableSet {
  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/ implements test.IMutableSet {
  @kotlin.IgnorableReturnValue()
  public boolean add(int);//  add(int)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(int);//  remove(int)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll();//  removeAll()

  @kotlin.IgnorableReturnValue()
  public boolean retainAll();//  retainAll()

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() test.IMutableSet);//  .ctor(test.IMutableSet)

  public boolean contains(int);//  contains(int)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public class CMutableSet3 /* test.CMutableSet3*/ implements test.IMutableSet {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CMutableSet3();//  .ctor()

  public boolean add(int);//  add(int)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.Integer>);//  addAll(java.util.Collection<? extends java.lang.Integer>)

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(int);//  remove(int)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  removeAll(java.util.Collection)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  retainAll(java.util.Collection)

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public abstract interface IMutableSet /* test.IMutableSet*/ extends java.util.Set<java.lang.Integer>, kotlin.collections.MutableSet<java.lang.Integer>, kotlin.jvm.internal.markers.KMutableSet {
}
