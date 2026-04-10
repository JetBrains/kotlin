public abstract interface A /* A*/<T>  {
  @kotlin.jvm.JvmSuppressWildcards()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() Out<T> foo();//  foo()
}

public abstract interface B /* B*/ {
  @kotlin.jvm.JvmSuppressWildcards()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() In<Open> foo();//  foo()
}

public final class Container /* Container*/ {
  @kotlin.jvm.JvmSuppressWildcards()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<Open> bar();//  bar()

  @kotlin.jvm.JvmSuppressWildcards()
  public final int bar(boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() java.lang.Long>, @kotlin.jvm.JvmSuppressWildcards() long);//  bar(boolean, @org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() java.lang.Long>, @kotlin.jvm.JvmSuppressWildcards() long)

  @kotlin.jvm.JvmSuppressWildcards()
  public final int foo(boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  foo(boolean, @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @kotlin.jvm.JvmSuppressWildcards()
  public final void deepOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>>);//  deepOpen(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>>)

  @kotlin.jvm.JvmSuppressWildcards()
  public final void zoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>>, @org.jetbrains.annotations.NotNull() @kotlin.jvm.JvmSuppressWildcards() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>);//  zoo(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>>, @kotlin.jvm.JvmSuppressWildcards() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() OutPair<Final, OutPair<Out<Final>, Out<Final>>> falseTrueFalse();//  falseTrueFalse()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() OutPair<Open, ? extends OutPair<Open, ? extends Out<Open>>> combination();//  combination()

  public  Container();//  .ctor()

  public final void simpleIn(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @kotlin.jvm.JvmWildcard() @org.jetbrains.annotations.Nullable() java.lang.Object>);//  simpleIn(@org.jetbrains.annotations.NotNull() In<? super @kotlin.jvm.JvmWildcard() @org.jetbrains.annotations.Nullable() java.lang.Object>)

  public final void simpleOut(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @kotlin.jvm.JvmWildcard() @org.jetbrains.annotations.NotNull() Final>);//  simpleOut(@org.jetbrains.annotations.NotNull() Out<? extends @kotlin.jvm.JvmWildcard() @org.jetbrains.annotations.NotNull() Final>)
}

public final class ContainerForPropertyAndAccessors /* ContainerForPropertyAndAccessors*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> bar;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() In<? super @kotlin.jvm.JvmWildcard() @org.jetbrains.annotations.Nullable() java.lang.Object> simpleIn;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Out<? extends @kotlin.jvm.JvmWildcard() @org.jetbrains.annotations.NotNull() Final> simpleOut;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>> deepOpen;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() In<? super java.lang.Object> getSimpleIn();//  getSimpleIn()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<? extends Final> getSimpleOut();//  getSimpleOut()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<Open> getBar();//  getBar()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<Open> getZoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>);//  getZoo(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<Out<Out<Open>>> getDeepOpen();//  getDeepOpen()

  public  ContainerForPropertyAndAccessors();//  .ctor()

  public final void setBar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>);//  setBar(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>)
}

public final class Final /* Final*/ {
  public  Final();//  .ctor()
}

@kotlin.jvm.JvmSuppressWildcards()
public final class HasAnnotation /* HasAnnotation*/ {
  public  HasAnnotation();//  .ctor()

  public final void doesNot(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>);//  doesNot(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>)

  public final void parameterDisagrees(@org.jetbrains.annotations.NotNull() @kotlin.jvm.JvmSuppressWildcards() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  parameterDisagrees(@kotlin.jvm.JvmSuppressWildcards() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>)
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
