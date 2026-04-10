public final class OriginalClass /* pack.OriginalClass*/ {
  public  OriginalClass();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.OriginalClass original;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final @org.jetbrains.annotations.Nullable() pack.OriginalClass companionPropertyWithValueClassType;

  private static final int companionProperty;

  @java.lang.Override()
  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  @java.lang.Override()
  public int hashCode();//  hashCode()

  @java.lang.Override()
  public java.lang.String toString();//  toString()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.OriginalClass getOriginal();//  getOriginal()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.OriginalClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.OriginalClass)

  @kotlin.jvm.JvmExposeBoxed()
  public final int getProperty();//  getProperty()

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithoutParameters();//  funWithoutParameters()

  class Companion ...

  class RegularObject ...
}

public static final class Companion /* pack.ValueClass.Companion*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.ValueClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.ValueClass getCompanionPropertyWithValueClassType();//  getCompanionPropertyWithValueClassType()

  private  Companion();//  .ctor()

  public final int getCompanionProperty();//  getCompanionProperty()

  public final void companionFunction();//  companionFunction()
}

public static final class RegularObject /* pack.ValueClass.RegularObject*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass.RegularObject INSTANCE;

  private  RegularObject();//  .ctor()
}
