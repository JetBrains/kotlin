public abstract interface A /* A*/<T>  {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() Out<T> foo();//  foo()
}

public abstract interface B /* B*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Open> foo();//  foo()
}

public final class Container /* Container*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> bar();//  bar()

  @<error>()
  public final int bar(boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() java.lang.Long>, @<error>() long);//  bar(boolean, @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() java.lang.Long>, @<error>() long)

  @<error>()
  public final int foo(boolean, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  foo(boolean, @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @<error>()
  public final void deepOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>);//  deepOpen(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>)

  @<error>()
  public final void zoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>, @org.jetbrains.annotations.NotNull() @<error>() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>);//  zoo(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>, @<error>() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>)

  @org.jetbrains.annotations.NotNull()
  public final @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Final, @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>, @org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final>>> falseTrueFalse();//  falseTrueFalse()

  @org.jetbrains.annotations.NotNull()
  public final @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Open, @<error>() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Open, @<error>() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>> combination();//  combination()

  public  Container();//  .ctor()

  public final void simpleIn(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<@<error>() @org.jetbrains.annotations.Nullable() java.lang.Object>);//  simpleIn(@org.jetbrains.annotations.NotNull() In<@<error>() @org.jetbrains.annotations.Nullable() java.lang.Object>)

  public final void simpleOut(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final>);//  simpleOut(@org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final>)
}

public final class ContainerForPropertyAndAccessors /* ContainerForPropertyAndAccessors*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open> bar;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() In<@<error>() @org.jetbrains.annotations.Nullable() java.lang.Object> simpleIn;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final> simpleOut;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>> deepOpen;

  @org.jetbrains.annotations.NotNull()
  public final @<error>() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> getZoo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>);//  getZoo(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() In<@<error>() @org.jetbrains.annotations.Nullable() java.lang.Object> getSimpleIn();//  getSimpleIn()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<@<error>() @org.jetbrains.annotations.NotNull() Final> getSimpleOut();//  getSimpleOut()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> getBar();//  getBar()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>> getDeepOpen();//  getDeepOpen()

  public  ContainerForPropertyAndAccessors();//  .ctor()

  public final void setBar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>);//  setBar(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>)
}

public final class Final /* Final*/ {
  public  Final();//  .ctor()
}

@<error>()
public final class HasAnnotation /* HasAnnotation*/ {
  public  HasAnnotation();//  .ctor()

  public final void doesNot(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>);//  doesNot(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>)

  public final void parameterDisagrees(@org.jetbrains.annotations.NotNull() @<error>() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  parameterDisagrees(@<error>() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() java.lang.Integer>)
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
