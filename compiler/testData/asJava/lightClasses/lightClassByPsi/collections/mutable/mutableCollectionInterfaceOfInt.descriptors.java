public abstract class CCollection /* test.CCollection*/ implements test.IMutableCollection {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/ implements test.IMutableCollection {
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

  public  CCollection2(@org.jetbrains.annotations.NotNull() test.IMutableCollection);//  .ctor(test.IMutableCollection)

  public boolean contains(int);//  contains(int)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public class CCollection3 /* test.CCollection3*/ implements test.IMutableCollection {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CCollection3();//  .ctor()

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

public abstract interface IMutableCollection /* test.IMutableCollection*/ extends java.util.Collection<java.lang.Integer>, kotlin.collections.MutableCollection<java.lang.Integer>, kotlin.jvm.internal.markers.KMutableCollection {
}
