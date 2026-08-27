public final class AllNullable /* AllNullable*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() IntWrapper b;

  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() StringWrapper a;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() IntWrapper getB();//  getB()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() StringWrapper getA();//  getA()

  @kotlin.jvm.JvmExposeBoxed()
  public  AllNullable(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() IntWrapper);//  .ctor(@org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.Nullable() IntWrapper)
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

public final class NoneNullable /* NoneNullable*/ {
  private final int a;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper getA();//  getA()

  @kotlin.jvm.JvmExposeBoxed()
  public  NoneNullable(@org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(@org.jetbrains.annotations.NotNull() IntWrapper)

  private  NoneNullable(int);//  .ctor(int)
}

public final class SomeNotNull /* SomeNotNull*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() StringWrapper a;

  private final int b;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper getB();//  getB()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() StringWrapper getA();//  getA()

  @kotlin.jvm.JvmExposeBoxed()
  public  SomeNotNull(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(@org.jetbrains.annotations.Nullable() StringWrapper, @org.jetbrains.annotations.NotNull() IntWrapper)

  private  SomeNotNull(@org.jetbrains.annotations.Nullable() StringWrapper, int);//  .ctor(@org.jetbrains.annotations.Nullable() StringWrapper, int)
}

@kotlin.jvm.JvmInline()
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
