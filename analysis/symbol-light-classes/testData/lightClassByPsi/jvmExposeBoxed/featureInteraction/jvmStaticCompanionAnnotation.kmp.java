@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Foo.Companion Companion;

  public  Foo();//  .ctor()

  class Companion ...
}

@<error>()
public static final class Companion /* Foo.Companion*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String foo();//  foo()

  private  Companion();//  .ctor()
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}
