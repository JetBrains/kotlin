public final class Foo /* Foo*/ {
  public  Foo();//  .ctor()

  public final int getFoo();//  getFoo()

  public final void setFoo(int);//  setFoo(int)
}

@<error>()
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  IntWrapper(int);//  .ctor(int)

  public final int getI();//  getI()
}
