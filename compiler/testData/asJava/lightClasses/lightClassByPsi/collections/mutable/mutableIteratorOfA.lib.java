public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class AMutableIterator /* test.AMutableIterator*/ implements java.util.Iterator<test.A>, kotlin.jvm.internal.markers.KMutableIterator {
  public  AMutableIterator();//  .ctor()
}

public abstract class AMutableIterator2 /* test.AMutableIterator2*/ implements java.util.Iterator<test.A>, kotlin.jvm.internal.markers.KMutableIterator {
  @org.jetbrains.annotations.NotNull()
  public test.A next();//  next()

  public  AMutableIterator2();//  .ctor()

  public boolean hasNext();//  hasNext()

  public void remove();//  remove()
}
