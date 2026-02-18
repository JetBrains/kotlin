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

public final class DelegatingBoolean /* DelegatingBoolean*/ implements BaseBoolean<@org.jetbrains.annotations.NotNull() java.lang.Boolean> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Boolean boolean();//  boolean()

  public  DelegatingBoolean(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() BaseBoolean<@org.jetbrains.annotations.NotNull() java.lang.Boolean>);//  .ctor(@org.jetbrains.annotations.NotNull() BaseBoolean<@org.jetbrains.annotations.NotNull() java.lang.Boolean>)
}

public final class DelegatingBooleanProperty /* DelegatingBooleanProperty*/ implements BaseBooleanProperty<@org.jetbrains.annotations.NotNull() java.lang.Boolean> {
  private final boolean booleanProperty;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Boolean getBooleanProperty();//  getBooleanProperty()

  public  DelegatingBooleanProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() BaseBooleanProperty<@org.jetbrains.annotations.NotNull() java.lang.Boolean>);//  .ctor(@org.jetbrains.annotations.NotNull() BaseBooleanProperty<@org.jetbrains.annotations.NotNull() java.lang.Boolean>)
}

public final class DelegatingInt /* DelegatingInt*/ implements BaseInt<@org.jetbrains.annotations.NotNull() java.lang.Integer> {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer int();//  int()

  public  DelegatingInt(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() BaseInt<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  .ctor(@org.jetbrains.annotations.NotNull() BaseInt<@org.jetbrains.annotations.NotNull() java.lang.Integer>)
}

public final class DelegatingIntProperty /* DelegatingIntProperty*/ implements BaseIntProperty<@org.jetbrains.annotations.NotNull() java.lang.Integer> {
  private final int intProperty;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer getIntProperty();//  getIntProperty()

  public  DelegatingIntProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() BaseIntProperty<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  .ctor(@org.jetbrains.annotations.NotNull() BaseIntProperty<@org.jetbrains.annotations.NotNull() java.lang.Integer>)
}
