public abstract class A /* A*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String abstractMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() int);//  abstractMethod(@org.jetbrains.annotations.NotNull() int)

  public  A();//  .ctor()
}

public final class B /* B*/ extends A {
  @<error>()
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String abstractMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() int);//  abstractMethod(@org.jetbrains.annotations.NotNull() int)

  public  B();//  .ctor()
}
