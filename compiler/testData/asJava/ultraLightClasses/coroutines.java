public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.Nullable()
  public final java.lang.Object doSomething(@org.jetbrains.annotations.NotNull() Foo, @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super Bar>);//  doSomething(Foo, kotlin.coroutines.Continuation<? super Bar>)

  public  Foo();//  .ctor()

}

public final class Boo /* Boo*/ {
  public  Boo();//  .ctor()

}

public final class Bar /* Bar*/ {
  public  Bar();//  .ctor()

  public final <T> void async(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super T>, ? extends java.lang.Object>);// <T>  async(kotlin.jvm.functions.Function1<? super kotlin.coroutines.Continuation<? super T>, ? extends java.lang.Object>)

}

public abstract interface Base /* Base*/ {
  @org.jetbrains.annotations.Nullable()
  public abstract java.lang.Object foo(@org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Unit>);//  foo(kotlin.coroutines.Continuation<? super kotlin.Unit>)

}

public final class Derived /* Derived*/ implements Base {
  @org.jetbrains.annotations.Nullable()
  public java.lang.Object foo(@org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Unit>);//  foo(kotlin.coroutines.Continuation<? super kotlin.Unit>)

  public  Derived();//  .ctor()

}