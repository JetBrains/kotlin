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
  public final Out<? extends Open> bar();//  bar()

  @kotlin.jvm.JvmSuppressWildcards(suppress = false)
  public final int foo(boolean, @org.jetbrains.annotations.NotNull() Out<? extends java.lang.Integer>);//  foo(boolean, Out<? extends java.lang.Integer>)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public final int bar(boolean, @org.jetbrains.annotations.NotNull() In<java.lang.Long>, @kotlin.jvm.JvmSuppressWildcards(suppress = false) long);//  bar(boolean, In<java.lang.Long>, @kotlin.jvm.JvmSuppressWildcards(suppress = false) long)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public final void deepOpen(@org.jetbrains.annotations.NotNull() Out<Out<Out<Open>>>);//  deepOpen(Out<Out<Out<Open>>>)

  @kotlin.jvm.JvmSuppressWildcards(suppress = true)
  public final void zoo(@org.jetbrains.annotations.NotNull() Out<Out<Out<Open>>>, @org.jetbrains.annotations.NotNull() @kotlin.jvm.JvmSuppressWildcards(suppress = false) Out<? extends Open>);//  zoo(Out<Out<Out<Open>>>, @kotlin.jvm.JvmSuppressWildcards(suppress = false) Out<? extends Open>)

  @org.jetbrains.annotations.NotNull()
  public final @kotlin.jvm.JvmSuppressWildcards() OutPair<Open, ? extends @kotlin.jvm.JvmWildcard() OutPair<Open, ? extends @kotlin.jvm.JvmWildcard() Out<Open>>> combination();//  combination()

  @org.jetbrains.annotations.NotNull()
  public final @kotlin.jvm.JvmSuppressWildcards(suppress = false) OutPair<? extends Final, @kotlin.jvm.JvmSuppressWildcards() OutPair<Out<Final>, Out<? extends @kotlin.jvm.JvmSuppressWildcards(suppress = false) Final>>> falseTrueFalse();//  falseTrueFalse()

  public  Container();//  .ctor()

  public final void simpleIn(@org.jetbrains.annotations.NotNull() In<? super @kotlin.jvm.JvmWildcard() java.lang.Object>);//  simpleIn(In<? super @kotlin.jvm.JvmWildcard() java.lang.Object>)

  public final void simpleOut(@org.jetbrains.annotations.NotNull() Out<? extends @kotlin.jvm.JvmWildcard() Final>);//  simpleOut(Out<? extends @kotlin.jvm.JvmWildcard() Final>)
}

public final class ContainerForPropertyAndAccessors /* ContainerForPropertyAndAccessors*/ {
  @org.jetbrains.annotations.NotNull()
  private Out<? extends Open> bar;

  @org.jetbrains.annotations.NotNull()
  private final In<? super @kotlin.jvm.JvmWildcard() java.lang.Object> simpleIn;

  @org.jetbrains.annotations.NotNull()
  private final Out<? extends @kotlin.jvm.JvmWildcard() Final> simpleOut;

  @org.jetbrains.annotations.NotNull()
  private final Out<Out<Out<Open>>> deepOpen;

  @org.jetbrains.annotations.NotNull()
  public final @kotlin.jvm.JvmSuppressWildcards(suppress = false) Out<? extends Open> getZoo(@org.jetbrains.annotations.NotNull() Out<? extends Out<? extends Out<? extends Open>>>);//  getZoo(Out<? extends Out<? extends Out<? extends Open>>>)

  @org.jetbrains.annotations.NotNull()
  public final In<? super @kotlin.jvm.JvmWildcard() java.lang.Object> getSimpleIn();//  getSimpleIn()

  @org.jetbrains.annotations.NotNull()
  public final Out<? extends @kotlin.jvm.JvmWildcard() Final> getSimpleOut();//  getSimpleOut()

  @org.jetbrains.annotations.NotNull()
  public final Out<Open> getBar();//  getBar()

  @org.jetbrains.annotations.NotNull()
  public final Out<Out<Out<Open>>> getDeepOpen();//  getDeepOpen()

  public  ContainerForPropertyAndAccessors();//  .ctor()

  public final void setBar(@org.jetbrains.annotations.NotNull() Out<? extends Open>);//  setBar(Out<? extends Open>)
}

public final class Final /* Final*/ {
  public  Final();//  .ctor()
}

@kotlin.jvm.JvmSuppressWildcards(suppress = true)
public final class HasAnnotation /* HasAnnotation*/ {
  public  HasAnnotation();//  .ctor()

  public final void doesNot(@org.jetbrains.annotations.NotNull() Out<Out<Open>>);//  doesNot(Out<Out<Open>>)

  public final void parameterDisagrees(@org.jetbrains.annotations.NotNull() @kotlin.jvm.JvmSuppressWildcards(suppress = false) Out<? extends java.lang.Integer>);//  parameterDisagrees(@kotlin.jvm.JvmSuppressWildcards(suppress = false) Out<? extends java.lang.Integer>)
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
