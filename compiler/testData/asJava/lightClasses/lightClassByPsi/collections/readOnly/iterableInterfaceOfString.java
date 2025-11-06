public abstract class CIterable /* test.CIterable*/ implements test.IIterable {
  public  CIterable();//  .ctor()
}

public abstract class CIterable2 /* test.CIterable2*/ implements test.IIterable {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  public  CIterable2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterable);//  .ctor(@org.jetbrains.annotations.NotNull() test.IIterable)
}

public class CIterable3 /* test.CIterable3*/ implements test.IIterable {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  public  CIterable3();//  .ctor()
}

public abstract interface IIterable /* test.IIterable*/ extends java.lang.Iterable<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
