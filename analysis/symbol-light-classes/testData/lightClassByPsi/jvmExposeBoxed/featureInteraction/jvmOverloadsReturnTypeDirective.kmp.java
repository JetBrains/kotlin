public final class Baz /* Baz*/ {
  @<error>()
  public final int memberLevel(int, int);//  memberLevel(int, int)

  public  Baz();//  .ctor()
}

@<error>()
public final class IntWrapper /* IntWrapper*/ {
  private final int s;

  @kotlin.jvm.JvmExposeBoxed()
  public  IntWrapper(int);//  .ctor(int)

  public final int getS();//  getS()
}

public final class JvmOverloadsReturnTypeDirectiveKt /* JvmOverloadsReturnTypeDirectiveKt*/ {
  @<error>()
  public static final int topLevel(int, int);//  topLevel(int, int)
}
