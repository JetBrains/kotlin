public final class OriginalClass /* pack.OriginalClass*/ {
  public  OriginalClass();//  .ctor()
}

@<error>()
public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.OriginalClass original;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final @org.jetbrains.annotations.Nullable() pack.OriginalClass companionPropertyWithValueClassType = null /* initializer type: null */;

  private static final int companionProperty = 0 /* initializer type: int */;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.OriginalClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.OriginalClass);//  .ctor(@org.jetbrains.annotations.NotNull() pack.OriginalClass)

  @kotlin.jvm.JvmExposeBoxed()
  public final int getProperty();//  getProperty()

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.OriginalClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.OriginalClass)

  @kotlin.jvm.JvmExposeBoxed()
  public final void funWithoutParameters();//  funWithoutParameters()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.OriginalClass getOriginal();//  getOriginal()

  class Companion ...

  class RegularObject ...
}

public static final class Companion /* pack.ValueClass.Companion*/ {
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.OriginalClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() pack.OriginalClass getCompanionPropertyWithValueClassType();//  getCompanionPropertyWithValueClassType()

  private  Companion();//  .ctor()

  public final int getCompanionProperty();//  getCompanionProperty()

  public final void companionFunction();//  companionFunction()
}

public static final class RegularObject /* pack.ValueClass.RegularObject*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass.RegularObject INSTANCE;

  private  RegularObject();//  .ctor()
}
