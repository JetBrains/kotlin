@<error>()
public final class MyClass /* MyClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  private  MyClass(@org.jetbrains.annotations.NotNull() int);//  .ctor(@org.jetbrains.annotations.NotNull() int)

  private  MyClass(@org.jetbrains.annotations.NotNull() int, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() int, @org.jetbrains.annotations.NotNull() java.lang.String)
}

@<error>()
public final class MyUInt /* MyUInt*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() int value;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
