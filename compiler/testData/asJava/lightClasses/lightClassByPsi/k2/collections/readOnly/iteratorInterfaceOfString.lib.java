public abstract class CIterator /* test.CIterator*/ implements test.IIterator {
  public  CIterator();//  .ctor()

  public void remove();//  remove()
}

public abstract class CIterator2 /* test.CIterator2*/ implements test.IIterator {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String next();//  next()

  public  CIterator2(@org.jetbrains.annotations.NotNull() test.IIterator);//  .ctor(test.IIterator)

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}

public class CIterator3 /* test.CIterator3*/ implements test.IIterator {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String next();//  next()

  public  CIterator3();//  .ctor()

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}

public abstract interface IIterator /* test.IIterator*/ extends java.util.Iterator<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
