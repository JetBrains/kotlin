public abstract class CIterator /* test.CIterator*/ implements test.IMutableIterator {
  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/ implements test.IMutableIterator {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String next();//  next()

  public  CIterator2(@org.jetbrains.annotations.NotNull() test.IMutableIterator);//  .ctor(test.IMutableIterator)

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}

public class CIterator3 /* test.CIterator3*/ implements test.IMutableIterator {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String next();//  next()

  public  CIterator3();//  .ctor()

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}

public abstract interface IMutableIterator /* test.IMutableIterator*/ extends java.util.Iterator<java.lang.String>, kotlin.jvm.internal.markers.KMutableIterator {
}
