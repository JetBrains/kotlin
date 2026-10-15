public final class DataClass /* pack.DataClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.ValueClass value;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.DataClass copy(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  copy(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass component1();//  component1()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getValue();//  getValue()

  public  DataClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class JvmExposeBoxedDirectiveKt /* pack.JvmExposeBoxedDirectiveKt*/ {
  @org.jetbrains.annotations.Nullable()
  private static @org.jetbrains.annotations.Nullable() pack.ValueClass topLevelProperty = null /* initializer type: null */;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass topLevelFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  topLevelFunction(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @org.jetbrains.annotations.Nullable()
  public static final @org.jetbrains.annotations.Nullable() pack.ValueClass getTopLevelProperty();//  getTopLevelProperty()

  public static final void setTopLevelProperty(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() pack.ValueClass);//  setTopLevelProperty(@org.jetbrains.annotations.Nullable() pack.ValueClass)
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.ValueClass value;

  @org.jetbrains.annotations.Nullable()
  private @org.jetbrains.annotations.Nullable() pack.ValueClass property = null /* initializer type: null */;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass function(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  function(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getValue();//  getValue()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.ValueClass getProperty();//  getProperty()

  public  Regular(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public final void setProperty(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() pack.ValueClass);//  setProperty(@org.jetbrains.annotations.Nullable() pack.ValueClass)
}

public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String first;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final @org.jetbrains.annotations.Nullable() pack.ValueClass companionPropertyWithValueClassType = null /* initializer type: null */;

  private final int second;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFirst();//  getFirst()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getProperty();//  getProperty()

  public final int getSecond();//  getSecond()

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public final void funWithoutParameters();//  funWithoutParameters()

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* pack.ValueClass.Companion*/ {
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.ValueClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.ValueClass getCompanionPropertyWithValueClassType();//  getCompanionPropertyWithValueClassType()

  private  Companion();//  .ctor()
}
