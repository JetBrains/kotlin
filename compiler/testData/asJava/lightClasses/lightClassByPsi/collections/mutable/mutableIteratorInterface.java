public abstract class CIterator /* test.CIterator*/<Elem>  implements test.IMutableIterator<Elem> {
  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/<Elem>  implements test.IMutableIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableIterator<Elem>)
}

public class CIterator3 /* test.CIterator3*/<Elem>  implements test.IMutableIterator<Elem> {
  @java.lang.Override()
  public Elem next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator3();//  .ctor()
}

public abstract interface IMutableIterator /* test.IMutableIterator*/<Elem>  extends java.util.Iterator<Elem>, kotlin.collections.MutableIterator<Elem>, kotlin.jvm.internal.markers.KMutableIterator {
}
