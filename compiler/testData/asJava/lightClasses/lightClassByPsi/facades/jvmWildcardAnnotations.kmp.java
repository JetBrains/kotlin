public final class Final /* Final*/ {
  public  Final();//  .ctor()
}

public final class In /* In*/<Z>  {
  public  In();//  .ctor()
}

public final class Inv /* Inv*/<E>  {
  public  Inv();//  .ctor()
}

public final class JvmWildcardAnnotationsKt /* JvmWildcardAnnotationsKt*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Open> foo3();//  foo3()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> bar();//  bar()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() T> foo2();//  foo2()

  @<error>()
  public static final int bar(boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() java.lang.Long>, @<error>() long);//  bar(boolean, @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() java.lang.Long>, @<error>() long)

  @<error>()
  public static final int foo(boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  foo(boolean, @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @<error>()
  public static final void deepOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>);//  deepOpen(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>)

  @org.jetbrains.annotations.NotNull()
  public static final @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Final, @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>, @org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final>>> falseTrueFalse();//  falseTrueFalse()

  @org.jetbrains.annotations.NotNull()
  public static final @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Open, @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Open, @<error>() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>> combination();//  combination()

  public static final void simpleIn(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<@<error>() @org.jetbrains.annotations.Nullable() java.lang.Object>);//  simpleIn(@org.jetbrains.annotations.NotNull() In<@<error>() @org.jetbrains.annotations.Nullable() java.lang.Object>)

  public static final void simpleOut(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final>);//  simpleOut(@org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final>)
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
