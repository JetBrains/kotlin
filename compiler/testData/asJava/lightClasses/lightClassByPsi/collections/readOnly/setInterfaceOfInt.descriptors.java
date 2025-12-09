public abstract class CSet /* test.CSet*/ implements test.ISet {
  public  CSet();//  .ctor()
}

public abstract class CSet2 /* test.CSet2*/ implements test.ISet {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CSet2(@org.jetbrains.annotations.NotNull() test.ISet);//  .ctor(test.ISet)

  public boolean contains(int);//  contains(int)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CSet3 /* test.CSet3*/ implements test.ISet {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  public  CSet3();//  .ctor()

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface ISet /* test.ISet*/ extends java.util.Set<java.lang.Integer>, kotlin.collections.Set<java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
