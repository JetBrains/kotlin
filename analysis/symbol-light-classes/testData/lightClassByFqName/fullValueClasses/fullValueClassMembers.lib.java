public final class ValueClass /* pack.ValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final pack.OriginalClass original;

  @org.jetbrains.annotations.NotNull()
  public static final pack.ValueClass.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private static final pack.ValueClass companionPropertyWithValueClassType;

  private final int count;

  private static final int companionProperty;

  @org.jetbrains.annotations.NotNull()
  public final pack.OriginalClass getOriginal();//  getOriginal()

  @org.jetbrains.annotations.NotNull()
  public final pack.ValueClass getPropertyWithValueClassType();//  getPropertyWithValueClassType()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  ValueClass(@org.jetbrains.annotations.NotNull() pack.OriginalClass, int);//  .ctor(pack.OriginalClass, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getCount();//  getCount()

  public final int getProperty();//  getProperty()

  public final int getPropertyWithAccessors();//  getPropertyWithAccessors()

  public final void funWithSelfParameter(@org.jetbrains.annotations.NotNull() pack.ValueClass);//  funWithSelfParameter(pack.ValueClass)

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
    public final pack.ValueClass companionFunctionWithValueClassType();//  companionFunctionWithValueClassType()

    @org.jetbrains.annotations.Nullable()
    public final pack.ValueClass getCompanionPropertyWithValueClassType();//  getCompanionPropertyWithValueClassType()

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
