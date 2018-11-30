public final class Prop /* Prop*/ {
  private final java.lang.Object someProp;

  @null()
  public  Prop();

}

public final class Fun /* Fun*/ {
  @null()
  public  Fun();

  private final java.lang.Object someFun();

}

public final class ArrayOfAnonymous /* ArrayOfAnonymous*/ {
  private final java.lang.Object[] a1;

  @null()
  public  ArrayOfAnonymous();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object[] getA1();

}

final class C /* C*/ {
  private final int y;

  private final kotlin.jvm.functions.Function0<java.lang.Object> initChild;

  @null()
  public  C(int);

  @org.jetbrains.annotations.NotNull()
  public final kotlin.jvm.functions.Function0<java.lang.Object> getInitChild();

  public final int getY();

}

public abstract class Super /* Super*/ {
  @null()
  public  Super();

  @org.jetbrains.annotations.Nullable()
  public abstract java.lang.Object getA();

}

public final class Sub /* Sub*/ extends Super {
  private final java.lang.Object[] a;

  @null()
  public  Sub();

  @org.jetbrains.annotations.NotNull()
  public java.lang.Object[] getA();

}

public final class ValidPublicSupertype /* ValidPublicSupertype*/ {
  private final java.lang.Runnable x;

  @null()
  public  ValidPublicSupertype();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();

}

public abstract interface I /* I*/ {
}

public final class InvalidPublicSupertype /* InvalidPublicSupertype*/ {
  private final java.lang.Runnable x;

  @null()
  public  InvalidPublicSupertype();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();

}
