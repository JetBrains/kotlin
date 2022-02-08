public final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  public static final C.Companion Companion;

  public  C();//  .ctor()



  class Companion ...

    class Factory ...

  }

public static final class Companion /* C.Companion*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  private java.lang.String x;

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getC(I);//  getC(I)

  @<error>()
  public final void foo();//  foo()

  @<error>()
  public final void setC(I, @org.jetbrains.annotations.NotNull() java.lang.String);//  setC(I, java.lang.String)

  @<error>()
  public final void setC1(@org.jetbrains.annotations.NotNull() java.lang.String);//  setC1(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getC1();//  getC1()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getX();//  getX()

  private  Companion();//  .ctor()

  public final void bar();//  bar()

  public final void setX(@org.jetbrains.annotations.NotNull() java.lang.String);//  setX(java.lang.String)

}

public static final class Factory /* C.Factory*/ {
  private  Factory();//  .ctor()

}

public final class C1 /* C1*/ {
  @org.jetbrains.annotations.NotNull()
  private static final C1.Companion Companion;

  public  C1();//  .ctor()


  class Companion ...

  }

private static final class Companion /* C1.Companion*/ {
  private  Companion();//  .ctor()

}

public abstract interface I /* I*/ {
  @org.jetbrains.annotations.NotNull()
  public static final I.Companion Companion;


  class Companion ...

  }

public static final class Companion /* I.Companion*/ {
  private  Companion();//  .ctor()

}

public final class Obj /* Obj*/ implements java.lang.Runnable {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  private static java.lang.String x;

  @org.jetbrains.annotations.NotNull()
  public static final Obj INSTANCE;

  @<error>()
  public final int zoo();//  zoo()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getX();//  getX()

  private  Obj();//  .ctor()

  public final void setX(@org.jetbrains.annotations.NotNull() java.lang.String);//  setX(java.lang.String)

  public void run();//  run()

}

public final class ConstContainer /* ConstContainer*/ {
  @org.jetbrains.annotations.NotNull()
  private static final java.lang.String str = "one" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  public static final ConstContainer INSTANCE;

  private static final double complexFloat = 5.118281745910645 /* initializer type: double */;

  private static final double e = 2.7182818284 /* initializer type: double */;

  private static final float eFloat = 2.7182817f /* initializer type: float */;

  private static final int one = 1 /* initializer type: int */;

  private static final long complexLong = 2L /* initializer type: long */;

  private static final long oneLong = 1L /* initializer type: long */;

  private  ConstContainer();//  .ctor()

}

public final class ClassWithConstContainer /* ClassWithConstContainer*/ {
  @org.jetbrains.annotations.NotNull()
  public static final ClassWithConstContainer.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String str = "one" /* initializer type: java.lang.String */;

  public static final double complexFloat = 5.118281745910645 /* initializer type: double */;

  public static final double e = 2.7182818284 /* initializer type: double */;

  public static final float eFloat = 2.7182817f /* initializer type: float */;

  public static final int one = 1 /* initializer type: int */;

  public static final long complexLong = 2L /* initializer type: long */;

  public static final long oneLong = 1L /* initializer type: long */;

  public  ClassWithConstContainer();//  .ctor()


  class Companion ...

  }

public static final class Companion /* ClassWithConstContainer.Companion*/ {
  private  Companion();//  .ctor()

}
