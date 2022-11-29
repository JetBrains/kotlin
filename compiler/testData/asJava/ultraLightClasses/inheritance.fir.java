public abstract interface Intf /* Intf*/ {
  public abstract int v();//  v()

}

public abstract interface IntfWithProp /* IntfWithProp*/ extends Intf {
  public abstract int getX();//  getX()

}

public abstract class Base /* Base*/ {
  private int y = 1 /* initializer type: int */;

  private int z = 1 /* initializer type: int */;

  @org.jetbrains.annotations.Nullable()
  protected java.lang.Integer v();//  v()

  protected int getZ();//  getZ()

  protected void setZ(int);//  setZ(int)

  public  Base(int);//  .ctor(int)

  public abstract int abs();//  abs()

  public final void nv();//  nv()

  public int getX$light_idea_test_case();//  getX$light_idea_test_case()

  public int getY();//  getY()

  public void setY(int);//  setY(int)

}

public final class Derived /* Derived*/ extends Base implements IntfWithProp {
  private final int x = 3 /* initializer type: int */;

  @java.lang.Override()
  protected error.NonExistentClass v();//  v()

  @java.lang.Override()
  public int abs();//  abs()

  public  Derived(int);//  .ctor(int)

  public int getX$light_idea_test_case();//  getX$light_idea_test_case()

}

public abstract class AnotherDerived /* AnotherDerived*/ extends Base {
  private final int x;

  private final int y;

  private final int z;

  @java.lang.Override()
  protected final void v();//  v()

  protected int getZ();//  getZ()

  public  AnotherDerived(int, int, int);//  .ctor(int, int, int)

  public abstract int getAbstractProp();//  getAbstractProp()

  public abstract void noReturn(@org.jetbrains.annotations.NotNull() java.lang.String);//  noReturn(java.lang.String)

  public int getX$light_idea_test_case();//  getX$light_idea_test_case()

  public int getY();//  getY()

}

final class Private /* Private*/ {
  public  Private();//  .ctor()

  public boolean getOverridesNothing();//  getOverridesNothing()

}
