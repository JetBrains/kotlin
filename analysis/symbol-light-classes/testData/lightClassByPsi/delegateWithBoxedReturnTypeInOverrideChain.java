public abstract interface BaseAny /* BaseAny*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Object getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Object int();//  int()
}

public abstract interface BaseInt /* BaseInt*/ extends BaseAny {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Integer getIntProperty();//  getIntProperty()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Integer int();//  int()
}

public final class DelegatingInt /* DelegatingInt*/ implements BaseInt {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getIntProperty();//  getIntProperty()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer int();//  int()

  public  DelegatingInt(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() BaseInt);//  .ctor(@org.jetbrains.annotations.NotNull() BaseInt)
}
