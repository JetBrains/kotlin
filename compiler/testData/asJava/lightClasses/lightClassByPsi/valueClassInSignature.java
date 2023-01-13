public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private java.lang.String classProp;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String classFunInReturn();//  classFunInReturn()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getClassProp();//  getClassProp()

  public  RegularClass();//  .ctor()

  public final int getClassPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  getClassPropInExtension(java.lang.String)

  public final void classFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  classFunInExtension(java.lang.String)

  public final void classFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String);//  classFunInParameter(java.lang.String)

  public final void setClassProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setClassProp(java.lang.String)

  public final void setClassPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  setClassPropInExtension(java.lang.String, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getInterfaceProp();//  getInterfaceProp()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String interfaceFunInReturn();//  interfaceFunInReturn()

  public abstract int getInterfacePropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  getInterfacePropInExtension(java.lang.String)

  public abstract void interfaceFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  interfaceFunInExtension(java.lang.String)

  public abstract void interfaceFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String);//  interfaceFunInParameter(java.lang.String)

  public abstract void setInterfaceProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setInterfaceProp(java.lang.String)

  public abstract void setInterfacePropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  setInterfacePropInExtension(java.lang.String, int)

  class DefaultImpls ...
}

@kotlin.jvm.JvmInline()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getValue();//  getValue()

  public  Some(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)
}
