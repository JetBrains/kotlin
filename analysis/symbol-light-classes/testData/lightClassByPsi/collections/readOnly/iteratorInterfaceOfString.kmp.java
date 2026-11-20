public abstract class CIterator /* test.CIterator*/ implements test.IIterator {
  public  CIterator();//  .ctor()
}

public abstract class CIterator2 /* test.CIterator2*/ implements test.IIterator {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  public  CIterator2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterator);//  .ctor(@org.jetbrains.annotations.NotNull() test.IIterator)
}

public class CIterator3 /* test.CIterator3*/ implements test.IIterator {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String next();//  next()

  @java.lang.Override()
  public boolean hasNext();//  hasNext()

  public  CIterator3();//  .ctor()
}

public abstract interface IIterator /* test.IIterator*/ extends java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
