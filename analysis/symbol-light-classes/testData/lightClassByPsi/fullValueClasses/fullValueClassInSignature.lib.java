public final class Another /* Another*/ {
  @kotlin.jvm.JvmOverloads()
  public  Another();//  .ctor()

  @kotlin.jvm.JvmOverloads()
  public  Another(@org.jetbrains.annotations.NotNull() Some);//  .ctor(Some)

  @kotlin.jvm.JvmOverloads()
  public  Another(@org.jetbrains.annotations.NotNull() Some, int);//  .ctor(Some, int)

  @kotlin.jvm.JvmOverloads()
  public  Another(@org.jetbrains.annotations.NotNull() Some, int, @org.jetbrains.annotations.NotNull() Some);//  .ctor(Some, int, Some)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private Some classProp;

  @org.jetbrains.annotations.NotNull()
  private Some classPropImplicit;

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public final void specialName();//  specialName()

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public final void specialName(int);//  specialName(int)

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public final void specialName(int, @org.jetbrains.annotations.NotNull() Some);//  specialName(int, Some)

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public final void specialName(int, @org.jetbrains.annotations.NotNull() Some, int);//  specialName(int, Some, int)

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public final void specialName(int, @org.jetbrains.annotations.NotNull() Some, int, @org.jetbrains.annotations.NotNull() Some);//  specialName(int, Some, int, Some)

  @kotlin.jvm.JvmOverloads()
  public  RegularClass();//  .ctor()

  @kotlin.jvm.JvmOverloads()
  public  RegularClass(int);//  .ctor(int)

  @kotlin.jvm.JvmOverloads()
  public  RegularClass(int, @org.jetbrains.annotations.NotNull() Some);//  .ctor(int, Some)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloads();//  withJvmOverloads()

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloads(int);//  withJvmOverloads(int)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloads(int, @org.jetbrains.annotations.NotNull() Some);//  withJvmOverloads(int, Some)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() Some);//  withJvmOverloadsAndValueReceiver(Some)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() Some, int);//  withJvmOverloadsAndValueReceiver(Some, int)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() Some, int, @org.jetbrains.annotations.NotNull() Some);//  withJvmOverloadsAndValueReceiver(Some, int, Some)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsButWithoutDefault(@org.jetbrains.annotations.NotNull() Some);//  withJvmOverloadsButWithoutDefault(Some)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsButWithoutDefault(@org.jetbrains.annotations.NotNull() Some, int);//  withJvmOverloadsButWithoutDefault(Some, int)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsInDifferentPositions();//  withJvmOverloadsInDifferentPositions()

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsInDifferentPositions(int);//  withJvmOverloadsInDifferentPositions(int)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() Some);//  withJvmOverloadsInDifferentPositions(int, Some)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() Some, int);//  withJvmOverloadsInDifferentPositions(int, Some, int)

  @kotlin.jvm.JvmOverloads()
  public final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() Some, int, @org.jetbrains.annotations.NotNull() Some);//  withJvmOverloadsInDifferentPositions(int, Some, int, Some)

  @org.jetbrains.annotations.NotNull()
  public final Some classFunInImplicitReturn();//  classFunInImplicitReturn()

  @org.jetbrains.annotations.NotNull()
  public final Some classFunInReturn();//  classFunInReturn()

  @org.jetbrains.annotations.NotNull()
  public final Some getClassProp();//  getClassProp()

  @org.jetbrains.annotations.NotNull()
  public final Some getClassPropImplicit();//  getClassPropImplicit()

  public final int getClassPropInExtension(@org.jetbrains.annotations.NotNull() Some);//  getClassPropInExtension(Some)

  public final void classFunInExtension(@org.jetbrains.annotations.NotNull() Some);//  classFunInExtension(Some)

  public final void classFunInParameter(@org.jetbrains.annotations.NotNull() Some);//  classFunInParameter(Some)

  public final void setClassProp(@org.jetbrains.annotations.NotNull() Some);//  setClassProp(Some)

  public final void setClassPropImplicit(@org.jetbrains.annotations.NotNull() Some);//  setClassPropImplicit(Some)

  public final void setClassPropInExtension(@org.jetbrains.annotations.NotNull() Some, int);//  setClassPropInExtension(Some, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract Some getInterfaceProp();//  getInterfaceProp()

  @org.jetbrains.annotations.NotNull()
  public abstract Some interfaceFunInReturn();//  interfaceFunInReturn()

  public abstract int getInterfacePropInExtension(@org.jetbrains.annotations.NotNull() Some);//  getInterfacePropInExtension(Some)

  public abstract void interfaceFunInExtension(@org.jetbrains.annotations.NotNull() Some);//  interfaceFunInExtension(Some)

  public abstract void interfaceFunInParameter(@org.jetbrains.annotations.NotNull() Some);//  interfaceFunInParameter(Some)

  public abstract void setInterfaceProp(@org.jetbrains.annotations.NotNull() Some);//  setInterfaceProp(Some)

  public abstract void setInterfacePropInExtension(@org.jetbrains.annotations.NotNull() Some, int);//  setInterfacePropInExtension(Some, int)
}

public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String value;

  private final int count;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  Some(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(java.lang.String, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getCount();//  getCount()

  public int hashCode();//  hashCode()
}
