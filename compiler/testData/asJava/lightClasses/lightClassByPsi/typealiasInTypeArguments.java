public abstract interface A /* A*/ {
}

public abstract interface B /* B*/<T, R>  {
}

public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() kotlin.jvm.functions.Function0<java.lang.Boolean> p;

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() kotlin.jvm.functions.Function0<java.lang.Boolean> getP();//  getP()

  public  Foo(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() kotlin.jvm.functions.Function0<java.lang.Boolean>);//  .ctor(@org.jetbrains.annotations.Nullable() kotlin.jvm.functions.Function0<java.lang.Boolean>)
}

public abstract interface Test /* Test*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() A foo();//  foo()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() A fooAliased();//  fooAliased()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() B<@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() B<@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String>> bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() B<@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() B<@org.jetbrains.annotations.NotNull() A, @org.jetbrains.annotations.NotNull() java.lang.String>> barAliased();//  barAliased()
}

public final class TypealiasInTypeArgumentsKt /* TypealiasInTypeArgumentsKt*/ {
}
