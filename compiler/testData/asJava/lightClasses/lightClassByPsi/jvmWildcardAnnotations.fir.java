public abstract interface A /* A*/<T>  {
  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  @org.jetbrains.annotations.NotNull()
  public abstract Out<T> foo();//  foo()
}

public abstract interface B /* B*/ {
  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  @org.jetbrains.annotations.NotNull()
  public abstract In<Open> foo();//  foo()
}

public final class Container /* Container*/ {
  @kotlin.jvm.JvmSuppressWildcards(suppress = false)
  @org.jetbrains.annotations.NotNull()
  public final Out<Open> bar();//  bar()

  @kotlin.jvm.JvmSuppressWildcards(suppress = false)
  public final int foo(boolean, @org.jetbrains.annotations.NotNull() Out<java.lang.Integer>);//  foo(boolean, Out<java.lang.Integer>)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public final int bar(boolean, @org.jetbrains.annotations.NotNull() In<? super java.lang.Long>, long);//  bar(boolean, In<? super java.lang.Long>, long)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public final void deepOpen(@org.jetbrains.annotations.NotNull() Out<? extends Out<? extends Out<? extends Open>>>);//  deepOpen(Out<? extends Out<? extends Out<? extends Open>>>)

  @org.jetbrains.annotations.NotNull()
  public final OutPair<Final, OutPair<Out<Final>, Out<Final>>> falseTrueFalse();//  falseTrueFalse()

  @org.jetbrains.annotations.NotNull()
  public final OutPair<Open, ? extends OutPair<Open,? extends Out<Open>>> combination();//  combination()

  public  Container();//  .ctor()

  public final void simpleIn(@org.jetbrains.annotations.NotNull() In<? super java.lang.Object>);//  simpleIn(In<? super java.lang.Object>)

  public final void simpleOut(@org.jetbrains.annotations.NotNull() Out<? extends Final>);//  simpleOut(Out<? extends Final>)
}

public final class Final /* Final*/ {
  public  Final();//  .ctor()
}

public final class In /* In*/<Z>  {
  public  In();//  .ctor()
}

public final class Inv /* Inv*/<E>  {
  public  Inv();//  .ctor()
}

public class Open /* Open*/ {
  public  Open();//  .ctor()
}

public final class Out /* Out*/<T>  {
  public  Out();//  .ctor()
}

public final class OutPair /* OutPair*/<Final, Y>  {
  public  OutPair();//  .ctor()
}
