public final class Exposed /* Exposed*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper renamed(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.String>);//  renamed(@org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper exposedName(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.String>);//  exposedName(@org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @kotlin.jvm.JvmName(name = "regularName")
  public final int regularName(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.String>);//  regularName(@org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.String>)

  public  Exposed();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}
