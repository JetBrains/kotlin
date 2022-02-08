public final class JvmWildcardAnnotationsKt /* JvmWildcardAnnotationsKt*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final In<Open> foo3();//  foo3()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final Out<Open> bar();//  bar()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final error.NonExistentClass foo2();//  foo2()

  @<error>()
  public static final int bar(boolean, @org.jetbrains.annotations.NotNull() In<? super java.lang.Long>, long);//  bar(boolean, In<? super java.lang.Long>, long)

  @<error>()
  public static final int foo(boolean, @org.jetbrains.annotations.NotNull() Out<java.lang.Integer>);//  foo(boolean, Out<java.lang.Integer>)

  @<error>()
  public static final void deepOpen(@org.jetbrains.annotations.NotNull() Out<? extends Out<? extends Out<? extends Open>>>);//  deepOpen(Out<? extends Out<? extends Out<? extends Open>>>)

  @org.jetbrains.annotations.NotNull()
  public static final OutPair<Final, OutPair<Out<Final>, Out<Final>>> falseTrueFalse();//  falseTrueFalse()

  @org.jetbrains.annotations.NotNull()
  public static final OutPair<Open, OutPair<Open, Out<Open>>> combination();//  combination()

  public static final void simpleIn(@org.jetbrains.annotations.NotNull() In<java.lang.Object>);//  simpleIn(In<java.lang.Object>)

  public static final void simpleOut(@org.jetbrains.annotations.NotNull() Out<? extends Final>);//  simpleOut(Out<? extends Final>)

}
