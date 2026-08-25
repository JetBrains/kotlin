public final class Another /* Another*/ {
  @<error>()
  public  Another(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String classProp;

  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String classPropImplicit;

  @<error>()
  @<error>()
  public final void withJvmOverloadsAndJvmName(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsAndJvmName(int, @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public  RegularClass(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void withJvmOverloads(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloads(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void withJvmOverloadsButWithoutDefault(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  withJvmOverloadsButWithoutDefault(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  @<error>()
  public final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String classFunInImplicitReturn();//  classFunInImplicitReturn()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String classFunInReturn();//  classFunInReturn()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getClassProp();//  getClassProp()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getClassPropImplicit();//  getClassPropImplicit()

  public final int getClassPropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getClassPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final void classFunInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  classFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final void classFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  classFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final void setClassProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setClassProp(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final void setClassPropImplicit(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setClassPropImplicit(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final void setClassPropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setClassPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getInterfaceProp();//  getInterfaceProp()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String interfaceFunInReturn();//  interfaceFunInReturn()

  public abstract int getInterfacePropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  getInterfacePropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void interfaceFunInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  interfaceFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void interfaceFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  interfaceFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void setInterfaceProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setInterfaceProp(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void setInterfacePropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setInterfacePropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int)
}

@<error>()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()
}
