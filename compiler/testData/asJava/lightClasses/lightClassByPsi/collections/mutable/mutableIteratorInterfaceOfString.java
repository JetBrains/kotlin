public abstract class CIterator /* test.CIterator*/ implements test.IMutableIterator {
  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/ implements test.IMutableIterator {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IMutableIterator);//  .ctor(@org.jetbrains.annotations.NotNull() test.IMutableIterator)
}

public class CIterator3 /* test.CIterator3*/ implements test.IMutableIterator {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  @java.lang.Override()
  public void remove();//  remove()

  public  CIterator3();//  .ctor()
}

public abstract interface IMutableIterator /* test.IMutableIterator*/ extends java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.MutableIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMutableIterator {
}
