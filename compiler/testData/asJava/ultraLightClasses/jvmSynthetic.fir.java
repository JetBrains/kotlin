public final class A /* A*/ {
  @<error>()
  @<error>()
  private int y;

  @<error>()
  private int x;

  private int z;

  @<error>()
  public final int getZ();//  getZ()

  @<error>()
  public final void foo();//  foo()

  @<error>()
  public final void setZ(int);//  setZ(int)

  public  A();//  .ctor()

  public final int getX();//  getX()

  public final int getY();//  getY()

  public final void setX(int);//  setX(int)

  public final void setY(int);//  setY(int)

}
