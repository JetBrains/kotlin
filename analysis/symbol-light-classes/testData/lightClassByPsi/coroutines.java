public final class Bar /* Bar*/ {
  public  Bar();//  .ctor()

  public final <T> void async(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super T>, ? extends java.lang.Object>);// <T>  async(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super T>, ? extends java.lang.Object>)
}

public abstract interface Base /* Base*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.Object foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>);//  foo(@org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>)
}

public final class Boo /* Boo*/ {
  private final @org.jetbrains.annotations.Nullable() java.lang.Object doSomething(@org.jetbrains.annotations.NotNull() Foo, @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() Bar>);//  doSomething(@org.jetbrains.annotations.NotNull() Foo, @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() Bar>)

  public  Boo();//  .ctor()
}

public final class Derived /* Derived*/ implements Base {
  @java.lang.Override()
  @org.jetbrains.annotations.Nullable()
  public @org.jetbrains.annotations.Nullable() java.lang.Object foo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>);//  foo(@org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Unit>)

  public  Derived();//  .ctor()
}

public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.Object doSomething(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Foo, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() Bar>);//  doSomething(@org.jetbrains.annotations.NotNull() Foo, @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() Bar>)

  public  Foo();//  .ctor()
}
