public final class Foo /* Foo*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Foo.Companion Companion;

  public  Foo();//  .ctor()

  class Companion ...
}

public static final class Companion /* Foo.Companion*/ {
  private  Companion();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;
}
