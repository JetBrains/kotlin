public abstract class CIterator /* test.CIterator*/<Elem>  implements test.IIterator<Elem> {
  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/<Elem>  implements test.IIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IIterator<Elem>)
}

public class CIterator3 /* test.CIterator3*/<Elem>  implements test.IIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator3();//  .ctor()
}

public abstract interface IIterator /* test.IIterator*/<Elem>  extends java.util.Iterator<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
