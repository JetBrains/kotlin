public abstract interface BaseAny /* BaseAny*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Object getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Object int();//  int()
}

public abstract interface BaseInt /* BaseInt*/ extends BaseAny {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Integer getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Integer int();//  int()
}

public final class DelegatingInt /* DelegatingInt*/ implements BaseInt {
  private final int intProperty;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer int();//  int()

  public  DelegatingInt(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() BaseInt);//  .ctor(@org.jetbrains.annotations.NotNull() BaseInt)
}
