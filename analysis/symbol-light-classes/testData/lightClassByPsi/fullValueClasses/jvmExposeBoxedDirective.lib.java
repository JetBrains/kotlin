public final class DataClass /* pack.DataClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final pack.ValueClass value;

  @org.jetbrains.annotations.NotNull()
  public final pack.DataClass copy(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  copy(pack.ValueClass)

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass component1();//  component1()

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  DataClass(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  .ctor(pack.ValueClass)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class JvmExposeBoxedDirectiveKt /* pack.JvmExposeBoxedDirectiveKt*/ {
  @org.jetbrains.annotations.Nullable()
  private static pack.ValueClass topLevelProperty;

  @org.jetbrains.annotations.NotNull()
  public static final pack.ValueClass topLevelFunction(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  topLevelFunction(pack.ValueClass)

  @org.jetbrains.annotations.Nullable()
  public static final pack.ValueClass getTopLevelProperty();//  getTopLevelProperty()

  public static final void setTopLevelProperty(@org.jetbrains.annotations.Nullable() pack.ValueClass);//  setTopLevelProperty(pack.ValueClass)
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final pack.ValueClass value;

  @org.jetbrains.annotations.Nullable()
  private pack.ValueClass property;

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass function(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  function(pack.ValueClass)

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass getValue();//  getValue()

  @org.jetbrains.annotations.Nullable()
  public final pack.ValueClass getProperty();//  getProperty()

  public  Regular(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  .ctor(pack.ValueClass)

  public final void setProperty(@org.jetbrains.annotations.Nullable() pack.ValueClass);//  setProperty(pack.ValueClass)
}

public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String first;

  @org.jetbrains.annotations.NotNull()
  public static final pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final pack.ValueClass companionPropertyWithValueClassType;

  private final int second;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getFirst();//  getFirst()

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  ValueClass(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(java.lang.String, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getProperty();//  getProperty()

  public final int getSecond();//  getSecond()

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(pack.ValueClass)

  public final void funWithoutParameters();//  funWithoutParameters()

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* pack.ValueClass.Companion*/ {
  @org.jetbrains.annotations.Nullable()
  public final pack.ValueClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

  @org.jetbrains.annotations.Nullable()
  public final pack.ValueClass getCompanionPropertyWithValueClassType();//  getCompanionPropertyWithValueClassType()

  private  Companion();//  .ctor()
}
