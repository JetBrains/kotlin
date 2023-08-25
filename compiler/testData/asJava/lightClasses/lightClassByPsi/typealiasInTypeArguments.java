public abstract interface A /* A*/ {
}

public abstract interface B /* B*/<T, R>  {
}

public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.Nullable()
  private final kotlin.jvm.functions.Function0<java.lang.Boolean> p;

  @org.jetbrains.annotations.Nullable()
  public final kotlin.jvm.functions.Function0<java.lang.Boolean> getP();//  getP()

  public  Foo(@org.jetbrains.annotations.Nullable() kotlin.jvm.functions.Function0<java.lang.Boolean>);//  .ctor(kotlin.jvm.functions.Function0<java.lang.Boolean>)
}

public abstract interface Test /* Test*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract A foo();//  foo()

  @org.jetbrains.annotations.NotNull()
  public abstract A fooAliased();//  fooAliased()

  @org.jetbrains.annotations.NotNull()
  public abstract B<A, B<A, java.lang.String>> bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public abstract B<A, B<A, java.lang.String>> barAliased();//  barAliased()
}

public final class TypealiasInTypeArgumentsKt /* TypealiasInTypeArgumentsKt*/ {
}
