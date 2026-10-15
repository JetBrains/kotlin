public final class JvmExposeBoxedAnnotationKt /* pack.JvmExposeBoxedAnnotationKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final pack.ValueClass topLevelFunction(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  topLevelFunction(pack.ValueClass)
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.Nullable()
  private pack.ValueClass property;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass function(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  function(pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.Nullable()
  public final pack.ValueClass getProperty();//  getProperty()

  @kotlin.jvm.JvmExposeBoxed()
  public final void setProperty(@org.jetbrains.annotations.Nullable() pack.ValueClass);//  setProperty(pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName")
  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass exposedName(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  exposedName(pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName2")
  @kotlin.jvm.JvmName(name = "jvmName")
  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass jvmName(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  jvmName(pack.ValueClass)

  public  Regular();//  .ctor()
}

public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String first;

  private final int second;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getFirst();//  getFirst()

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  ValueClass(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(java.lang.String, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getSecond();//  getSecond()

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(pack.ValueClass)

  public int hashCode();//  hashCode()
}
