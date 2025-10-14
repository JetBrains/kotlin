public abstract class CIterator /* test.CIterator*/<Elem>  implements test.IIterator<Elem> {
  public  CIterator();//  .ctor()

  public void remove();//  remove()
}

public abstract class CIterator2 /* test.CIterator2*/<Elem>  implements test.IIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IIterator<Elem>)

  public void remove();//  remove()
}

public class CIterator3 /* test.CIterator3*/<Elem>  implements test.IIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  public  CIterator3();//  .ctor()

  public void remove();//  remove()
}

public abstract interface IIterator /* test.IIterator*/<Elem>  extends java.util.Iterator<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
