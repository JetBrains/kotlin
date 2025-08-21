public final class Baz /* Baz*/ {
  @<error>()
  @<error>()
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper memberLevel(int, int);//  memberLevel(int, int)

  public  Baz();//  .ctor()
}

@<error>()
public final class IntWrapper /* IntWrapper*/ {
  private final int s;

  @kotlin.jvm.JvmExposeBoxed()
  public  IntWrapper(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getS();//  getS()

  public int hashCode();//  hashCode()
}

public final class JvmOverloadsReturnTypeJvmNameDirectiveKt /* JvmOverloadsReturnTypeJvmNameDirectiveKt*/ {
  @<error>()
  @<error>()
  public static final int topLevel(int, int);//  topLevel(int, int)
}
