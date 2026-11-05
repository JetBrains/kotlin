public abstract interface BaseAny /* BaseAny*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Object getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Object int();//  int()
}

public abstract interface BaseInt /* BaseInt*/ extends BaseAny {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Integer getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Integer int();//  int()
}

public final class DelegatingInt /* DelegatingInt*/ implements BaseInt {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getIntProperty();//  getIntProperty()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer int();//  int()

  public  DelegatingInt(@org.jetbrains.annotations.NotNull() BaseInt);//  .ctor(BaseInt)
}
