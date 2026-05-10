public abstract class CIterable /* test.CIterable*/<Elem>  implements test.IMutableIterable<Elem> {
  public  CIterable();//  .ctor()
}

public abstract class CIterable2 /* test.CIterable2*/<Elem>  implements test.IMutableIterable<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CIterable2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableIterable<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableIterable<Elem>)
}

public class CIterable3 /* test.CIterable3*/<Elem>  implements test.IMutableIterable<Elem> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<Elem> iterator();//  iterator()

  public  CIterable3();//  .ctor()
}

public abstract interface IMutableIterable /* test.IMutableIterable*/<Elem>  extends java.lang.Iterable<Elem>, kotlin.jvm.internal.markers.KMutableIterable {
}
