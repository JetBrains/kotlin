public final class A /* A*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  private final java.util.Collection<?> a;

  @<error>()
  private int b;

  @org.jetbrains.annotations.NotNull()
  public static final A.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public final java.util.Collection<?> getA();//  getA()

  public  A();//  .ctor()

  public final int getB();//  getB()

  public final void setB(int);//  setB(int)


  class Companion ...

  }

public static final class Companion /* A.Companion*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  private final java.util.Collection<?> c;

  @<error>()
  private int d;

  @org.jetbrains.annotations.NotNull()
  public final java.util.Collection<?> getC();//  getC()

  private  Companion();//  .ctor()

  public final int getD();//  getD()

  public final void setD(int);//  setD(int)

}

public abstract interface B /* B*/ {
  @org.jetbrains.annotations.NotNull()
  public static final B.Companion Companion;


  class Companion ...

  }

public static final class Companion /* B.Companion*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  private final java.util.Collection<?> a;

  @org.jetbrains.annotations.NotNull()
  public final java.util.Collection<?> getA();//  getA()

  private  Companion();//  .ctor()

}

public final class C /* C*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  private final java.util.Collection<?> a;

  @<error>()
  private int b;

  @org.jetbrains.annotations.NotNull()
  public final java.util.Collection<?> getA();//  getA()

  public  C();//  .ctor()

  public  C(@<error>() @org.jetbrains.annotations.NotNull() java.util.Collection<?>, @<error>() int);//  .ctor(java.util.Collection<?>, int)

  public final int getB();//  getB()

  public final void setB(int);//  setB(int)

}
