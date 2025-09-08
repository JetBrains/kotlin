@kotlin.jvm.JvmInline()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getValue();//  getValue()

  public  Some(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)
}

public final class SomeClass /* SomeClass*/ {
  @org.jetbrains.annotations.NotNull()
  private java.lang.String memberProp;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getMemberProp();//  getMemberProp()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String memberFunInReturn();//  memberFunInReturn()

  public  SomeClass();//  .ctor()

  public final int getMemberPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  getMemberPropInExtension(java.lang.String)

  public final void memberFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  memberFunInExtension(java.lang.String)

  public final void memberFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String);//  memberFunInParameter(java.lang.String)

  public final void setMemberProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setMemberProp(java.lang.String)

  public final void setMemberPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  setMemberPropInExtension(java.lang.String, int)
}

public abstract interface SomeInterface /* SomeInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getMemberProp();//  getMemberProp()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String memberFunInReturn();//  memberFunInReturn()

  public abstract int getMemberPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  getMemberPropInExtension(java.lang.String)

  public abstract void memberFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  memberFunInExtension(java.lang.String)

  public abstract void memberFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String);//  memberFunInParameter(java.lang.String)

  public abstract void setMemberProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setMemberProp(java.lang.String)

  public abstract void setMemberPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  setMemberPropInExtension(java.lang.String, int)

  public static final class DefaultImpls /* SomeInterface.DefaultImpls*/ {
    public static int getMemberPropInExtension(@org.jetbrains.annotations.NotNull() SomeInterface, @org.jetbrains.annotations.NotNull() java.lang.String);//  getMemberPropInExtension(SomeInterface, java.lang.String)

    public static void setMemberPropInExtension(@org.jetbrains.annotations.NotNull() SomeInterface, @org.jetbrains.annotations.NotNull() java.lang.String, int);//  setMemberPropInExtension(SomeInterface, java.lang.String, int)
  }
}

public final class ValueClassInSignatureKt /* ValueClassInSignatureKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static java.lang.String topLevelProp;

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public static final void specialName();//  specialName()

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public static final void specialName(int);//  specialName(int)

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public static final void specialName(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  specialName(int, java.lang.String)

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public static final void specialName(int, @org.jetbrains.annotations.NotNull() java.lang.String, int);//  specialName(int, java.lang.String, int)

  @kotlin.jvm.JvmName(name = "specialName")
  @kotlin.jvm.JvmOverloads()
  public static final void specialName(int, @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String);//  specialName(int, java.lang.String, int, java.lang.String)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloads();//  withJvmOverloads()

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloads(int);//  withJvmOverloads(int)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloads(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloads(int, java.lang.String)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsAndValueReceiver(java.lang.String)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  withJvmOverloadsAndValueReceiver(java.lang.String, int)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsAndValueReceiver(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsAndValueReceiver(java.lang.String, int, java.lang.String)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsButWithoutDefault(@org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsButWithoutDefault(java.lang.String)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsButWithoutDefault(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  withJvmOverloadsButWithoutDefault(java.lang.String, int)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsInDifferentPositions();//  withJvmOverloadsInDifferentPositions()

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsInDifferentPositions(int);//  withJvmOverloadsInDifferentPositions(int)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsInDifferentPositions(int, java.lang.String)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() java.lang.String, int);//  withJvmOverloadsInDifferentPositions(int, java.lang.String, int)

  @kotlin.jvm.JvmOverloads()
  public static final void withJvmOverloadsInDifferentPositions(int, @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String);//  withJvmOverloadsInDifferentPositions(int, java.lang.String, int, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getTopLevelProp();//  getTopLevelProp()

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String topLevelFunInReturn();//  topLevelFunInReturn()

  public static final int getTopLevelPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  getTopLevelPropInExtension(java.lang.String)

  public static final void setTopLevelProp(@org.jetbrains.annotations.NotNull() java.lang.String);//  setTopLevelProp(java.lang.String)

  public static final void setTopLevelPropInExtension(@org.jetbrains.annotations.NotNull() java.lang.String, int);//  setTopLevelPropInExtension(java.lang.String, int)

  public static final void topLevelFunInExtension(@org.jetbrains.annotations.NotNull() java.lang.String);//  topLevelFunInExtension(java.lang.String)

  public static final void topLevelFunInParameter(@org.jetbrains.annotations.NotNull() java.lang.String);//  topLevelFunInParameter(java.lang.String)
}
