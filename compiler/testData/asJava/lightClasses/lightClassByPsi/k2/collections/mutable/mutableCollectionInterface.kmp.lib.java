public abstract class CCollection /* test.CCollection*/<Elem>  implements test.IMutableCollection<Elem> {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  implements test.IMutableCollection<Elem> {
  private final int size;

  @kotlin.IgnorableReturnValue()
  public boolean add(Elem);//  add(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean remove(Elem);//  remove(Elem)

  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CCollection2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableCollection<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableCollection<Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public class CCollection3 /* test.CCollection3*/<Elem>  implements test.IMutableCollection<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public boolean add(Elem);//  add(Elem)

  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean contains(Elem);//  contains(Elem)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean isEmpty();//  isEmpty()

  public boolean remove(Elem);//  remove(Elem)

  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends Elem>)

  public int getSize();//  getSize()

  public void clear();//  clear()
}

public abstract interface IMutableCollection /* test.IMutableCollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.collections.MutableCollection<Elem>, kotlin.jvm.internal.markers.KMutableCollection {
}
