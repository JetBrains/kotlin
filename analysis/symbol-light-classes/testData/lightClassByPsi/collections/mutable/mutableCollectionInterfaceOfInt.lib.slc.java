public abstract class CCollection /* test.CCollection*/ implements test.IMutableCollection {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/ implements test.IMutableCollection {
  private final int size;

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  @kotlin.IgnorableReturnValue()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public void clear();//  clear()

  @kotlin.IgnorableReturnValue()
  public boolean add(int);//  add(int)

  @kotlin.IgnorableReturnValue()
  public boolean remove(int);//  remove(int)

  public  CCollection2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableCollection);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableCollection)

  public boolean contains(int);//  contains(int)

  public int getSize();//  getSize()
}

public class CCollection3 /* test.CCollection3*/ implements test.IMutableCollection {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @java.lang.Override()
  public boolean addAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  addAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public boolean removeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  removeAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean retainAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  retainAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public void clear();//  clear()

  public  CCollection3();//  .ctor()

  public boolean add(int);//  add(int)

  public boolean contains(int);//  contains(int)

  public boolean remove(int);//  remove(int)

  public int getSize();//  getSize()
}

public abstract interface IMutableCollection /* test.IMutableCollection*/ extends java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.collections.MutableCollection<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableCollection {
}
