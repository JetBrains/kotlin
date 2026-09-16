public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() pack.OriginalClass original;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final @org.jetbrains.annotations.Nullable() pack.ValueClass companionPropertyWithValueClassType;

  private final int count;

  private static final int companionProperty;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.OriginalClass getOriginal();//  getOriginal()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  public  ValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.OriginalClass, int);//  .ctor(@org.jetbrains.annotations.NotNull() pack.OriginalClass, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getCount();//  getCount()

  public final int getProperty();//  getProperty()

  public final int getPropertyWithAccessors();//  getPropertyWithAccessors()

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass)

  public final void funWithoutParameters();//  funWithoutParameters()

  public final void setPropertyWithAccessors(int);//  setPropertyWithAccessors(int)

  public int hashCode();//  hashCode()

  public final class Inner /* pack.ValueClass.Inner*/ {
    private final int nested;

    public  Inner(int);//  .ctor(int)

    public final int getNested();//  getNested()
  }

  public static final class Companion /* pack.ValueClass.Companion*/ {
    @org.jetbrains.annotations.Nullable()
    public final @org.jetbrains.annotations.Nullable() pack.ValueClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

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
}
