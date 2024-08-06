public final class A /* A*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<?> a;

  @org.jetbrains.annotations.NotNull()
  private static final @org.jetbrains.annotations.NotNull() java.util.Collection<?> c;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() A.Companion Companion;

  private int b = 1 /* initializer type: int */;

  private static int d = 1 /* initializer type: int */;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.util.Collection<?> getA();//  getA()

  public  A();//  .ctor()

  public final int getB();//  getB()

  public final void setB(int);//  setB(int)

  class Companion ...
}

public static final class Companion /* A.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.util.Collection<?> getC();//  getC()

  private  Companion();//  .ctor()

  public final int getD();//  getD()

  public final void setD(int);//  setD(int)
}

public abstract interface B /* B*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() B.Companion Companion;

  class Companion ...
}

public static final class Companion /* B.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  private static final @org.jetbrains.annotations.NotNull() java.util.Collection<?> a;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.util.Collection<?> getA();//  getA()

  private  Companion();//  .ctor()
}

public final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.Collection<?> a;

  private int b = 1 /* initializer type: int */;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.util.Collection<?> getA();//  getA()

  public  C();//  .ctor()

  public  C(@<error>() @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<?>, @<error>() int);//  .ctor(@org.jetbrains.annotations.NotNull() java.util.Collection<?>, int)

  public final int getB();//  getB()

  public final void setB(int);//  setB(int)
}
