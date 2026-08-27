public abstract interface Interface /* pack.Interface*/ {
  public abstract int getValue();//  getValue()
}

@<error>()
public final class ValueClass /* pack.ValueClass*/ implements pack.Interface {
  private final int value;

  @java.lang.Override()
  public int getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(int);//  .ctor(int)
}
