public abstract class CIterable /* test.CIterable*/ implements test.IIterable {
  public  CIterable();//  .ctor()

  public java.util.Iterator<java.lang.String> iterator();//  iterator()
}

public abstract class CIterable2 /* test.CIterable2*/ implements test.IIterable {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CIterable2(@org.jetbrains.annotations.NotNull() test.IIterable);//  .ctor(test.IIterable)
}

public class CIterable3 /* test.CIterable3*/ implements test.IIterable {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CIterable3();//  .ctor()
}

public abstract interface IIterable /* test.IIterable*/ extends java.lang.Iterable<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
