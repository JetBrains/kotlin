public abstract class CCollection /* test.CCollection*/<Elem>  implements test.ICollection<Elem> {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  public  CCollection2();//  .ctor()
}

public abstract class CCollection3 /* test.CCollection3*/ extends test.Foo implements java.util.Collection<java.lang.String>, kotlin.collections.Collection<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  public  CCollection3();//  .ctor()
}

public abstract class CCollection4 /* test.CCollection4*/ extends test.Foo implements test.ICollection<java.lang.String> {
  public  CCollection4();//  .ctor()
}

public abstract class CCollection5 /* test.CCollection5*/ extends test.CCollection4 {
  public  CCollection5();//  .ctor()
}

public final class CCollection6 /* test.CCollection6*/ implements java.util.Collection<java.lang.String>, kotlin.collections.Collection<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public static final test.CCollection6 INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  private  CCollection6();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract class Foo /* test.Foo*/ {
  public  Foo();//  .ctor()
}

public abstract interface ICollection /* test.ICollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.collections.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
