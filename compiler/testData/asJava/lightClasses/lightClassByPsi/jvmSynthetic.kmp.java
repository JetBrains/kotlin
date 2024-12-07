public final class A /* A*/ {
  private int x = 1 /* initializer type: int */;

  private int y = 1 /* initializer type: int */;

  private int z = 1 /* initializer type: int */;

  public @org.jetbrains.annotations.NotNull() A f;

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() A getF();//  getF()

  @<error>()
  public final int getZ();//  getZ()

  @<error>()
  public final void foo();//  foo()

  @<error>()
  public final void setZ(int);//  setZ(int)

  public  A();//  .ctor()

  public final int getX();//  getX()

  public final int getY();//  getY()

  public final void setF(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() A);//  setF(@org.jetbrains.annotations.NotNull() A)

  public final void setX(int);//  setX(int)

  public final void setY(int);//  setY(int)
}
