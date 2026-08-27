public final class Baz /* Baz*/ {
  @<error>()
  @<error>()
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  public  Baz(int, int);//  .ctor(int, int)
}

@<error>()
public final class IntWrapper /* IntWrapper*/ {
  private final int s;

  public final int getS();//  getS()
}
