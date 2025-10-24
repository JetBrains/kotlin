public final class A /* test.A*/ {
  public  A();//  .ctor()
}

public abstract class ACollection /* test.ACollection*/ implements java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  public  ACollection();//  .ctor()
}

public abstract class ACollection2 /* test.ACollection2*/ implements java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>, kotlin.jvm.internal.markers.KMappedMarker {
  private final int size;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() test.A> iterator();//  iterator()

  public  ACollection2();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.A);//  contains(@org.jetbrains.annotations.NotNull() test.A)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() test.A>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}
