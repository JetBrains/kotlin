public abstract class CMutableSet /* test.CMutableSet*/ implements test.IMutableSet {
  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/ implements test.IMutableSet {
  @kotlin.IgnorableReturnValue()
  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll();//  removeAll()

  @kotlin.IgnorableReturnValue()
  public boolean retainAll();//  retainAll()

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() test.IMutableSet);//  .ctor(test.IMutableSet)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public class CMutableSet3 /* test.CMutableSet3*/ implements test.IMutableSet {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CMutableSet3();//  .ctor()

  public boolean add(@org.jetbrains.annotations.NotNull() java.lang.String);//  add(java.lang.String)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends java.lang.String>);//  addAll(java.util.Collection<? extends java.lang.String>)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);//  remove(java.lang.String)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  removeAll(java.util.Collection)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  retainAll(java.util.Collection)

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public abstract interface IMutableSet /* test.IMutableSet*/ extends java.util.Set<java.lang.String>, kotlin.collections.MutableSet<java.lang.String>, kotlin.jvm.internal.markers.KMutableSet {
}
