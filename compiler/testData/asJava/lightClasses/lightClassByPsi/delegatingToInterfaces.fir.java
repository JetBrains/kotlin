public abstract interface Base /* Base*/ {
  public abstract int foo(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.Nullable() java.lang.Object);//  foo(java.lang.String, java.lang.Object)

  public abstract int getX();//  getX()

  public abstract int getY();//  getY()

  public abstract void printMessage();//  printMessage()

  public abstract void printMessageLine();//  printMessageLine()

  public abstract void setY(int);//  setY(int)
}

public final class BaseImpl /* BaseImpl*/ implements Base {
  private final int x;

  @java.lang.Override()
  public final int getX();//  getX()

  @java.lang.Override()
  public void printMessage();//  printMessage()

  @java.lang.Override()
  public void printMessageLine();//  printMessageLine()

  public  BaseImpl(int);//  .ctor(int)
}

public final class Derived /* Derived*/ implements Base {
  @java.lang.Override()
  public int foo(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.Nullable() java.lang.Object);//  foo(java.lang.String, java.lang.Object)

  @java.lang.Override()
  public void printMessage();//  printMessage()

  @java.lang.Override()
  public void printMessageLine();//  printMessageLine()

  public  Derived(@org.jetbrains.annotations.NotNull() Base);//  .ctor(Base)
}
