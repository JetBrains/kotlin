public final class Container /* Container*/ {
  @org.jetbrains.annotations.NotNull()
  public final In<Final> notDeepIn();//  notDeepIn()

  @org.jetbrains.annotations.NotNull()
  public final Inv<? super Out<? extends Open>> skipWildcardsUntilInProjection();//  skipWildcardsUntilInProjection()

  @org.jetbrains.annotations.NotNull()
  public final Inv<In<Out<? extends Open>>> skipAllInvWildcards();//  skipAllInvWildcards()

  @org.jetbrains.annotations.NotNull()
  public final Inv<OutPair<Open, Out<Out<Open>>>> skipAllOutInvWildcards();//  skipAllOutInvWildcards()

  @org.jetbrains.annotations.NotNull()
  public final Out<In<Out<? extends Open>>> skipWildcardsUntilIn0();//  skipWildcardsUntilIn0()

  @org.jetbrains.annotations.NotNull()
  public final Out<In<Out<Final>>> skipWildcardsUntilIn1();//  skipWildcardsUntilIn1()

  @org.jetbrains.annotations.NotNull()
  public final Out<In<OutPair<Final, ? extends Out<? extends Open>>>> skipWildcardsUntilIn2();//  skipWildcardsUntilIn2()

  public  Container();//  .ctor()

  public final <Q extends Final> void typeParameter(@org.jetbrains.annotations.NotNull() Out<? extends Q>, @org.jetbrains.annotations.NotNull() In<? super Q>);// <Q extends Final>  typeParameter(Out<? extends Q>, In<? super Q>)

  public final void arrayOfOutFinal(@org.jetbrains.annotations.NotNull() Out<Final>[]);//  arrayOfOutFinal(Out<Final>[])

  public final void arrayOfOutOpen(@org.jetbrains.annotations.NotNull() Out<Open>[]);//  arrayOfOutOpen(Out<Open>[])

  public final void deepFinal(@org.jetbrains.annotations.NotNull() Out<Out<Out<Final>>>);//  deepFinal(Out<Out<Out<Final>>>)

  public final void deepOpen(@org.jetbrains.annotations.NotNull() Out<? extends Out<? extends Out<? extends Open>>>);//  deepOpen(Out<? extends Out<? extends Out<? extends Open>>>)

  public final void finalClassArgument(@org.jetbrains.annotations.NotNull() Out<Final>, @org.jetbrains.annotations.NotNull() In<? super Final>);//  finalClassArgument(Out<Final>, In<? super Final>)

  public final void inAny(@org.jetbrains.annotations.NotNull() In<java.lang.Object>);//  inAny(In<java.lang.Object>)

  public final void inFinal(@org.jetbrains.annotations.NotNull() In<? super Final>);//  inFinal(In<? super Final>)

  public final void inOutFinal(@org.jetbrains.annotations.NotNull() In<? super Out<Final>>);//  inOutFinal(In<? super Out<Final>>)

  public final void invFinal(@org.jetbrains.annotations.NotNull() Inv<Final>);//  invFinal(Inv<Final>)

  public final void invIn(@org.jetbrains.annotations.NotNull() Out<? extends Inv<? super Final>>);//  invIn(Out<? extends Inv<? super Final>>)

  public final void invInAny(@org.jetbrains.annotations.NotNull() Out<Inv<? super java.lang.Object>>);//  invInAny(Out<Inv<? super java.lang.Object>>)

  public final void invInOutFinal(@org.jetbrains.annotations.NotNull() Inv<In<Out<Final>>>);//  invInOutFinal(Inv<In<Out<Final>>>)

  public final void invInOutOpen(@org.jetbrains.annotations.NotNull() Inv<In<Out<? extends Open>>>);//  invInOutOpen(Inv<In<Out<? extends Open>>>)

  public final void invInv(@org.jetbrains.annotations.NotNull() Out<Inv<Open>>);//  invInv(Out<Inv<Open>>)

  public final void invOpen(@org.jetbrains.annotations.NotNull() Inv<Open>);//  invOpen(Inv<Open>)

  public final void invOut(@org.jetbrains.annotations.NotNull() Out<? extends Inv<? extends Open>>);//  invOut(Out<? extends Inv<? extends Open>>)

  public final void invOutFinal(@org.jetbrains.annotations.NotNull() Inv<Out<Final>>);//  invOutFinal(Inv<Out<Final>>)

  public final void invOutFinal(@org.jetbrains.annotations.NotNull() Out<Inv<? extends Final>>);//  invOutFinal(Out<Inv<? extends Final>>)

  public final void invOutOpen(@org.jetbrains.annotations.NotNull() Inv<Out<Open>>);//  invOutOpen(Inv<Out<Open>>)

  public final void invOutProjectedOutFinal(@org.jetbrains.annotations.NotNull() Inv<? extends Out<Final>>);//  invOutProjectedOutFinal(Inv<? extends Out<Final>>)

  public final void oneArgumentFinal(@org.jetbrains.annotations.NotNull() OutPair<Final, ? extends Open>);//  oneArgumentFinal(OutPair<Final, ? extends Open>)

  public final void openClassArgument(@org.jetbrains.annotations.NotNull() Out<? extends Open>, @org.jetbrains.annotations.NotNull() In<? super Open>);//  openClassArgument(Out<? extends Open>, In<? super Open>)

  public final void outIn(@org.jetbrains.annotations.NotNull() Out<? extends In<? super Final>>);//  outIn(Out<? extends In<? super Final>>)

  public final void outInAny(@org.jetbrains.annotations.NotNull() Out<In<java.lang.Object>>);//  outInAny(Out<In<java.lang.Object>>)

  public final void outOfArrayOpen(@org.jetbrains.annotations.NotNull() Out<Open[]>);//  outOfArrayOpen(Out<Open[]>)

  public final void outOfArrayOutOpen(@org.jetbrains.annotations.NotNull() Out<? extends Open[]>);//  outOfArrayOutOpen(Out<? extends Open[]>)
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
