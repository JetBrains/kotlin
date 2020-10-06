public final class Prop /* Prop*/ {
  private final java.lang.Object someProp;

  public  Prop();//  .ctor()

}

final class null /* null*/ {
  private  ();//  .ctor()

}

final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  private final kotlin.jvm.functions.Function0<java.lang.Object> initChild;

  private final int y;

  @org.jetbrains.annotations.NotNull()
  public final kotlin.jvm.functions.Function0<java.lang.Object> getInitChild();//  getInitChild()

  public  C(int);//  .ctor(int)

  public final int getY();//  getY()

}

final class null /* null*/ {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  ();//  .ctor()

}

public final class ValidPublicSupertype /* ValidPublicSupertype*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.Runnable x;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();//  getX()

  public  ValidPublicSupertype();//  .ctor()

}

final class null /* null*/ implements java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}

final class null /* null*/ implements java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}

public abstract interface I /* I*/ {
}

public final class InvalidPublicSupertype /* InvalidPublicSupertype*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.Runnable x;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Runnable getX();//  getX()

  public  InvalidPublicSupertype();//  .ctor()

}

final class null /* null*/ implements I, java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}

final class null /* null*/ implements I, java.lang.Runnable {
  private  ();//  .ctor()

  public void run();//  run()

}