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

  public  BaseImpl(int);//  .ctor(int)

  public final int getX();//  getX()

  public void printMessage();//  printMessage()

  public void printMessageLine();//  printMessageLine()
}

public final class Derived /* Derived*/ implements Base {
  public  Derived(@org.jetbrains.annotations.NotNull() Base);//  .ctor(Base)

  public int foo(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.Nullable() java.lang.Object);//  foo(java.lang.String, java.lang.Object)

  public int getX();//  getX()

  public int getY();//  getY()

  public void printMessage();//  printMessage()

  public void printMessageLine();//  printMessageLine()

  public void setY(int);//  setY(int)
}
