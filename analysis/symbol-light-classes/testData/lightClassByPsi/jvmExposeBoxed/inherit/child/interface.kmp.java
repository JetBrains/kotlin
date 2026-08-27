public abstract interface A /* A*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String abstractMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() int);//  abstractMethod(@org.jetbrains.annotations.NotNull() int)
}

public final class B /* B*/ implements A {
  @<error>()
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String abstractMethod(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() int);//  abstractMethod(@org.jetbrains.annotations.NotNull() int)

  public  B();//  .ctor()
}
