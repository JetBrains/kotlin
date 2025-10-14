public abstract class CIterator /* test.CIterator*/<Elem>  implements test.IMutableIterator<Elem> {
  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/<Elem>  implements test.IMutableIterator<Elem> {
  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableIterator<Elem>)

  public Elem next();//  next()

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}

public class CIterator3 /* test.CIterator3*/<Elem>  implements test.IMutableIterator<Elem> {
  public  CIterator3();//  .ctor()

  public Elem next();//  next()

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}

public abstract interface IMutableIterator /* test.IMutableIterator*/<Elem>  extends java.util.Iterator<Elem>, kotlin.jvm.internal.markers.KMutableIterator {
}
