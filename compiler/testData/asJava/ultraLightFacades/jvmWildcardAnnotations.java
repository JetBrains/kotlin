public final class JvmWildcardAnnotationsKt /* JvmWildcardAnnotationsKt*/ {
  @kotlin.jvm.JvmSuppressWildcards(suppress = false)
  @org.jetbrains.annotations.NotNull()
  public static final Out<? extends Open> bar();//  bar()

  @kotlin.jvm.JvmSuppressWildcards(suppress = false)
  public static final int foo(boolean, @org.jetbrains.annotations.NotNull() Out<? extends java.lang.Integer>);//  foo(boolean, Out<? extends java.lang.Integer>)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  @org.jetbrains.annotations.NotNull()
  public static final In<Open> foo3();//  foo3()

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  @org.jetbrains.annotations.NotNull()
  public static final Out<error.NonExistentClass> foo2();//  foo2()

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public static final int bar(boolean, @org.jetbrains.annotations.NotNull() In<java.lang.Long>, long);//  bar(boolean, In<java.lang.Long>, long)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public static final void deepOpen(@org.jetbrains.annotations.NotNull() Out<Out<Out<Open>>>);//  deepOpen(Out<Out<Out<Open>>>)

  @org.jetbrains.annotations.NotNull()
  public static final OutPair<? extends Final, OutPair<Out<Final>, Out<? extends Final>>> falseTrueFalse();//  falseTrueFalse()

  @org.jetbrains.annotations.NotNull()
  public static final OutPair<Open, ? extends OutPair<Open,? extends Out<Open>>> combination();//  combination()

  public static final void simpleIn(@org.jetbrains.annotations.NotNull() In<? super java.lang.Object>);//  simpleIn(In<? super java.lang.Object>)

  public static final void simpleOut(@org.jetbrains.annotations.NotNull() Out<? extends Final>);//  simpleOut(Out<? extends Final>)

}