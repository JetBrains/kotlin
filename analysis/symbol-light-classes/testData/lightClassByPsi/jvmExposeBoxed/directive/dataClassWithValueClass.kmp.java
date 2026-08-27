public final class MyDataClass /* one.MyDataClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() one.MyDataClass copy(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  copy(@org.jetbrains.annotations.NotNull() one.MyValueClass)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() one.MyValueClass component1();//  component1()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() one.MyValueClass getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  MyDataClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  .ctor(@org.jetbrains.annotations.NotNull() one.MyValueClass)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  MyDataClass(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

@<error>()
public final class MyValueClass /* one.MyValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String str;

  @kotlin.jvm.JvmExposeBoxed()
  public  MyValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getStr();//  getStr()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class NullableDataClass /* one.NullableDataClass*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() java.lang.String value;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() one.NullableDataClass copy(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() one.MyValueClass);//  copy(@org.jetbrains.annotations.Nullable() one.MyValueClass)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() one.MyValueClass component1();//  component1()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() one.MyValueClass getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  NullableDataClass(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() one.MyValueClass);//  .ctor(@org.jetbrains.annotations.Nullable() one.MyValueClass)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  NullableDataClass(@org.jetbrains.annotations.Nullable() java.lang.String);//  .ctor(@org.jetbrains.annotations.Nullable() java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class NullableUnderlyingDataClass /* one.NullableUnderlyingDataClass*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass value;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() one.NullableUnderlyingDataClass copy(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass);//  copy(@org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass component1();//  component1()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass getValue();//  getValue()

  @kotlin.jvm.JvmExposeBoxed()
  public  NullableUnderlyingDataClass(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass);//  .ctor(@org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  NullableUnderlyingDataClass(@org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass);//  .ctor(@org.jetbrains.annotations.Nullable() one.NullableUnderlyingValueClass)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

@<error>()
public final class NullableUnderlyingValueClass /* one.NullableUnderlyingValueClass*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() java.lang.String str;

  @kotlin.jvm.JvmExposeBoxed()
  public  NullableUnderlyingValueClass(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String);//  .ctor(@org.jetbrains.annotations.Nullable() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.String getStr();//  getStr()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
