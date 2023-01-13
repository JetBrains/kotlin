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

  class DefaultImpls ...
}

public final class ValueClassInSignatureKt /* ValueClassInSignatureKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static java.lang.String topLevelProp;

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
