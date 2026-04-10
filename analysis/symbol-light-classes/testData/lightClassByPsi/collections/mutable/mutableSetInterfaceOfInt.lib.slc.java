public abstract class CMutableSet /* test.CMutableSet*/ implements test.IMutableSet {
  public  CMutableSet();//  .ctor()
}

public abstract class CMutableSet2 /* test.CMutableSet2*/ implements test.IMutableSet {
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

  public  CMutableSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableSet);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableSet)

  public boolean contains(int);//  contains(int)

  public int getSize();//  getSize()
}

public class CMutableSet3 /* test.CMutableSet3*/ implements test.IMutableSet {
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

  public  CMutableSet3();//  .ctor()

  public boolean add(int);//  add(int)

  public boolean contains(int);//  contains(int)

  public boolean remove(int);//  remove(int)

  public int getSize();//  getSize()
}

public abstract interface IMutableSet /* test.IMutableSet*/ extends java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.collections.MutableSet<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMutableSet {
}
