@<error>()
public final class AllNullable /* AllNullable*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() IntWrapper b;

  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() StringWrapper a;

  private  AllNullable(@org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.Nullable() IntWrapper);//  .ctor(@org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.Nullable() IntWrapper)
}

@<error>()
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}

@<error>()
public final class NoneNullable /* NoneNullable*/ {
  private final int a;

  private  NoneNullable(int);//  .ctor(int)
}

@<error>()
public final class SomeNotNull /* SomeNotNull*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() StringWrapper a;

  private final int b;

  private  SomeNotNull(@org.jetbrains.annotations.Nullable() StringWrapper, int);//  .ctor(@org.jetbrains.annotations.Nullable() StringWrapper, int)
}

@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
