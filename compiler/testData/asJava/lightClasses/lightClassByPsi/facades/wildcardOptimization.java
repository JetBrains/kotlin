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

public final class WildcardOptimizationKt /* WildcardOptimizationKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Final> notDeepIn();//  notDeepIn()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Inv<? super @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>> skipWildcardsUntilInProjection();//  skipWildcardsUntilInProjection()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>> skipAllInvWildcards();//  skipAllInvWildcards()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Open, @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>>> skipAllOutInvWildcards();//  skipAllOutInvWildcards()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>> skipWildcardsUntilIn0();//  skipWildcardsUntilIn0()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>> skipWildcardsUntilIn1();//  skipWildcardsUntilIn1()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Final, ? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>> skipWildcardsUntilIn2();//  skipWildcardsUntilIn2()

  public static final <Q extends Final> void typeParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Q>, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Q>);// <Q extends Final>  typeParameter(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Q>, @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Q>)

  public static final void arrayOfOutFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final> @org.jetbrains.annotations.NotNull() []);//  arrayOfOutFinal(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final> @org.jetbrains.annotations.NotNull() [])

  public static final void arrayOfOutOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> @org.jetbrains.annotations.NotNull() []);//  arrayOfOutOpen(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open> @org.jetbrains.annotations.NotNull() [])

  public static final void deepFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>>);//  deepFinal(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>>)

  public static final void deepOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>);//  deepOpen(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>)

  public static final void finalClassArgument(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Final>);//  finalClassArgument(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>, @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Final>)

  public static final void inAny(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() java.lang.Object>);//  inAny(@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() java.lang.Object>)

  public static final void inFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Final>);//  inFinal(@org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Final>)

  public static final void inOutFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>);//  inOutFinal(@org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>)

  public static final void invFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Final>);//  invFinal(@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Final>)

  public static final void invIn(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Inv<? super @org.jetbrains.annotations.NotNull() Final>>);//  invIn(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Inv<? super @org.jetbrains.annotations.NotNull() Final>>)

  public static final void invInAny(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Inv<? super @org.jetbrains.annotations.NotNull() java.lang.Object>>);//  invInAny(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Inv<? super @org.jetbrains.annotations.NotNull() java.lang.Object>>)

  public static final void invInOutFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>>);//  invInOutFinal(@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>>)

  public static final void invInOutOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>);//  invInOutOpen(@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>>>)

  public static final void invInv(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Open>>);//  invInv(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Open>>)

  public static final void invOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Open>);//  invOpen(@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Open>)

  public static final void invOut(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Inv<? extends @org.jetbrains.annotations.NotNull() Open>>);//  invOut(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Inv<? extends @org.jetbrains.annotations.NotNull() Open>>)

  public static final void invOutFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>);//  invOutFinal(@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>)

  public static final void invOutFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Inv<? extends @org.jetbrains.annotations.NotNull() Final>>);//  invOutFinal(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Inv<? extends @org.jetbrains.annotations.NotNull() Final>>)

  public static final void invOutOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>);//  invOutOpen(@org.jetbrains.annotations.NotNull() Inv<@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open>>)

  public static final void invOutProjectedOutFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Inv<? extends @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>);//  invOutProjectedOutFinal(@org.jetbrains.annotations.NotNull() Inv<? extends @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Final>>)

  public static final void oneArgumentFinal(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Final, ? extends @org.jetbrains.annotations.NotNull() Open>);//  oneArgumentFinal(@org.jetbrains.annotations.NotNull() OutPair<@org.jetbrains.annotations.NotNull() Final, ? extends @org.jetbrains.annotations.NotNull() Open>)

  public static final void openClassArgument(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Open>);//  openClassArgument(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open>, @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Open>)

  public static final void outIn(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Final>>);//  outIn(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() In<? super @org.jetbrains.annotations.NotNull() Final>>)

  public static final void outInAny(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.Nullable() java.lang.Object>>);//  outInAny(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() In<@org.jetbrains.annotations.Nullable() java.lang.Object>>)

  public static final void outOfArrayOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open @org.jetbrains.annotations.NotNull() []>);//  outOfArrayOpen(@org.jetbrains.annotations.NotNull() Out<@org.jetbrains.annotations.NotNull() Open @org.jetbrains.annotations.NotNull() []>)

  public static final void outOfArrayOutOpen(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open @org.jetbrains.annotations.NotNull() []>);//  outOfArrayOutOpen(@org.jetbrains.annotations.NotNull() Out<? extends @org.jetbrains.annotations.NotNull() Open @org.jetbrains.annotations.NotNull() []>)
}
