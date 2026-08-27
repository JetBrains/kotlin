public final class Baz /* Baz*/ {
  @<error>()
  @<error>()
  @<error>()
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  public final int memberLevel(int, int);//  memberLevel(int, int)

  public  Baz();//  .ctor()
}

@<error>()
public final class IntWrapper /* IntWrapper*/ {
  private final int s;

  public final int getS();//  getS()
}

public final class JvmOverloadsReturnTypeJvmNameKt /* JvmOverloadsReturnTypeJvmNameKt*/ {
  @<error>()
  @<error>()
  @<error>()
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  public static final int topLevel(int, int);//  topLevel(int, int)
}
