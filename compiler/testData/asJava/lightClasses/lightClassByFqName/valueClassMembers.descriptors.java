@kotlin.jvm.JvmInline()
public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final pack.OriginalClass original;

  @org.jetbrains.annotations.NotNull()
  public static final pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final pack.OriginalClass companionPropertyWithValueClassType;

  private static final int companionProperty;

  @org.jetbrains.annotations.NotNull()
  public final pack.OriginalClass getOriginal();//  getOriginal()

  @org.jetbrains.annotations.NotNull()
  public final pack.OriginalClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  public  ValueClass(@org.jetbrains.annotations.NotNull() pack.OriginalClass);//  .ctor(pack.OriginalClass)

  public final int getProperty();//  getProperty()

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.OriginalClass);//  funWithSelfParameter(pack.OriginalClass)

  public final void funWithoutParameters();//  funWithoutParameters()

  public static final class Companion /* pack.ValueClass.Companion*/ {
    @org.jetbrains.annotations.Nullable()
    public final pack.OriginalClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

    @org.jetbrains.annotations.Nullable()
    public final pack.OriginalClass getCompanionPropertyWithValueClassType();//  getCompanionPropertyWithValueClassType()

    private  Companion();//  .ctor()

    public final int getCompanionProperty();//  getCompanionProperty()

    public final void companionFunction();//  companionFunction()
  }

  public static final class RegularObject /* pack.ValueClass.RegularObject*/ {
    @org.jetbrains.annotations.NotNull()
    public static final pack.ValueClass.RegularObject INSTANCE;

    private  RegularObject();//  .ctor()
  }
}