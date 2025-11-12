public abstract class CMutableSet /* test.CMutableSet*/<Elem>  implements test.IMutableSet<Elem> {
  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/<Elem>  implements test.IMutableSet<Elem> {
  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(Elem);//  remove(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll();//  removeAll()

  @kotlin.IgnorableReturnValue()
  public boolean retainAll();//  retainAll()

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() test.IMutableSet<Elem>);//  .ctor(test.IMutableSet<Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public class CMutableSet3 /* test.CMutableSet3*/<Elem>  implements test.IMutableSet<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CMutableSet3();//  .ctor()

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(java.util.Collection<? extends Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(Elem);//  remove(Elem)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  removeAll(java.util.Collection)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  retainAll(java.util.Collection)

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public abstract interface IMutableSet /* test.IMutableSet*/<Elem>  extends java.util.Set<Elem>, kotlin.collections.MutableSet<Elem>, kotlin.jvm.internal.markers.KMutableSet {
}
