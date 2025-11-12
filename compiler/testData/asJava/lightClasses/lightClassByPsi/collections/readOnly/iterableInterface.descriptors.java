public abstract class CIterable /* test.CIterable*/<Elem>  implements test.IIterable<Elem> {
  public  CIterable();//  .ctor()
}

public abstract class CIterable2 /* test.CIterable2*/<Elem>  implements test.IIterable<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CIterable2(@org.jetbrains.annotations.NotNull() test.IIterable<Elem>);//  .ctor(test.IIterable<Elem>)
}

public class CIterable3 /* test.CIterable3*/<Elem>  implements test.IIterable<Elem> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<Elem> iterator();//  iterator()

  public  CIterable3();//  .ctor()
}

public abstract interface IIterable /* test.IIterable*/<Elem>  extends java.lang.Iterable<Elem>, kotlin.collections.Iterable<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
