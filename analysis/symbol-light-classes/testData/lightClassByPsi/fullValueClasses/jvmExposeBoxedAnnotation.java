@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class JvmExposeBoxedAnnotationKt /* pack.JvmExposeBoxedAnnotationKt*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass topLevelFunction(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  topLevelFunction(@org.jetbrains.annotations.NotNull() pack.ValueClass)
}

public final class Regular /* pack.Regular*/ {
  @org.jetbrains.annotations.Nullable()
  private @org.jetbrains.annotations.Nullable() pack.ValueClass property = null /* initializer type: null */;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass function(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  function(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.ValueClass getProperty();//  getProperty()

  @kotlin.jvm.JvmExposeBoxed()
  public final void setProperty(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() pack.ValueClass);//  setProperty(@org.jetbrains.annotations.Nullable() pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass exposedName(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  exposedName(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName2")
  @kotlin.jvm.JvmName(name = "jvmName")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass jvmName(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  jvmName(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public  Regular();//  .ctor()
}

public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String first;

  private final int second;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getFirst();//  getFirst()

  public final int getSecond();//  getSecond()
}
