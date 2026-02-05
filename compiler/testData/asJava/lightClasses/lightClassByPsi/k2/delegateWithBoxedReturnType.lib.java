public abstract interface BaseBoolean /* BaseBoolean*/<T>  {
  public abstract T boolean();//  boolean()
}

public abstract interface BaseBooleanProperty /* BaseBooleanProperty*/<T>  {
  public abstract T getBooleanProperty();//  getBooleanProperty()
}

public abstract interface BaseInt /* BaseInt*/<T>  {
  public abstract T int();//  int()
}

public abstract interface BaseIntProperty /* BaseIntProperty*/<T>  {
  public abstract T getIntProperty();//  getIntProperty()
}

public final class DelegatingBoolean /* DelegatingBoolean*/ implements BaseBoolean<java.lang.Boolean> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Boolean boolean();//  boolean()

  public  DelegatingBoolean(@org.jetbrains.annotations.NotNull() BaseBoolean<java.lang.Boolean>);//  .ctor(BaseBoolean<java.lang.Boolean>)
}

public final class DelegatingBooleanProperty /* DelegatingBooleanProperty*/ implements BaseBooleanProperty<java.lang.Boolean> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Boolean getBooleanProperty();//  getBooleanProperty()

  public  DelegatingBooleanProperty(@org.jetbrains.annotations.NotNull() BaseBooleanProperty<java.lang.Boolean>);//  .ctor(BaseBooleanProperty<java.lang.Boolean>)
}

public final class DelegatingInt /* DelegatingInt*/ implements BaseInt<java.lang.Integer> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer int();//  int()

  public  DelegatingInt(@org.jetbrains.annotations.NotNull() BaseInt<java.lang.Integer>);//  .ctor(BaseInt<java.lang.Integer>)
}

public final class DelegatingIntProperty /* DelegatingIntProperty*/ implements BaseIntProperty<java.lang.Integer> {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer getIntProperty();//  getIntProperty()

  public  DelegatingIntProperty(@org.jetbrains.annotations.NotNull() BaseIntProperty<java.lang.Integer>);//  .ctor(BaseIntProperty<java.lang.Integer>)
}
