public abstract class CIterable /* test.CIterable*/ implements test.IMutableIterable {
  public  CIterable();//  .ctor()
}

public abstract class CIterable2 /* test.CIterable2*/ implements test.IMutableIterable {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CIterable2(@org.jetbrains.annotations.NotNull() test.IMutableIterable);//  .ctor(test.IMutableIterable)
}

public class CIterable3 /* test.CIterable3*/ implements test.IMutableIterable {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  public  CIterable3();//  .ctor()
}

public abstract interface IMutableIterable /* test.IMutableIterable*/ extends java.lang.Iterable<java.lang.String>, kotlin.jvm.internal.markers.KMutableIterable {
}
