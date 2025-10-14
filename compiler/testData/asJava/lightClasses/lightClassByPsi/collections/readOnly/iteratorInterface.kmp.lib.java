public abstract class CIterator /* test.CIterator*/<Elem>  implements test.IIterator<Elem> {
  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/<Elem>  implements test.IIterator<Elem> {
  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterator<Elem>);//  .ctor(@org.jetbrains.annotations.NotNull() test.IIterator<Elem>)

  public Elem next();//  next()

  public boolean hasNext();//  hasNext()
}

public class CIterator3 /* test.CIterator3*/<Elem>  implements test.IIterator<Elem> {
  public  CIterator3();//  .ctor()

  public Elem next();//  next()

  public boolean hasNext();//  hasNext()
}

public abstract interface IIterator /* test.IIterator*/<Elem>  extends java.util.Iterator<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}
