public abstract class CCollection /* test.CCollection*/ implements test.ICollection {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/ implements test.ICollection {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CCollection2(@org.jetbrains.annotations.NotNull() test.ICollection);//  .ctor(test.ICollection)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CCollection3 /* test.CCollection3*/ implements test.ICollection {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface ICollection /* test.ICollection*/ extends java.util.Collection<java.lang.String>, kotlin.collections.Collection<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
