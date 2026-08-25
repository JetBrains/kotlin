@<error>()
@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String baz;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Foo.Companion Companion;

  public  Foo();//  .ctor()

  class Companion ...
}

public static final class Companion /* Foo.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getBaz();//  getBaz()

  private  Companion();//  .ctor()

  public final void setBaz(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setBaz(@org.jetbrains.annotations.NotNull() java.lang.String)
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}
