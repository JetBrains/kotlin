public final class ClassWithResultConstructor /* ClassWithResultConstructor*/ {
  public  ClassWithResultConstructor(@org.jetbrains.annotations.NotNull() java.lang.Object);//  .ctor(java.lang.Object)
}

public final class ClassWithValueClassConstructor /* ClassWithValueClassConstructor*/ {
  private  ClassWithValueClassConstructor(java.lang.String);//  .ctor(java.lang.String)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private java.lang.Object classResultProp;

  @org.jetbrains.annotations.Nullable()
  private kotlin.Result<java.lang.Integer> classNullableResultProp;

  @kotlin.jvm.JvmName(name = "resultInReturnWithJvmName")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object resultInReturnWithJvmName();//  resultInReturnWithJvmName()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object classResultInReturn-d1pmJ48();//  classResultInReturn-d1pmJ48()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object getClassResultProp-d1pmJ48();//  getClassResultProp-d1pmJ48()

  @org.jetbrains.annotations.Nullable()
  public final java.lang.Object classSuspendResultInReturn-IoAF18A(@org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super kotlin.Result<java.lang.Integer>>);//  classSuspendResultInReturn-IoAF18A(kotlin.coroutines.Continuation<? super kotlin.Result<java.lang.Integer>>)

  @org.jetbrains.annotations.Nullable()
  public final kotlin.Result<java.lang.Integer> classNullableResultInReturn-xLWZpok();//  classNullableResultInReturn-xLWZpok()

  @org.jetbrains.annotations.Nullable()
  public final kotlin.Result<java.lang.Integer> getClassNullableResultProp-xLWZpok();//  getClassNullableResultProp-xLWZpok()

  public  RegularClass();//  .ctor()

  public final int getClassPropInResultExtension(@org.jetbrains.annotations.NotNull() java.lang.Object);//  getClassPropInResultExtension(java.lang.Object)

  public final void classResultAndValueClassInParameter-NpkG7VQ(@org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.String);//  classResultAndValueClassInParameter-NpkG7VQ(java.lang.Object, java.lang.String)

  public final void classResultInContext(@org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInContext(java.lang.Object)

  public final void classResultInExtension(@org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInExtension(java.lang.Object)

  public final void classResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInParameter(java.lang.Object)

  public final void setClassNullableResultProp(@org.jetbrains.annotations.Nullable() kotlin.Result<java.lang.Integer>);//  setClassNullableResultProp(kotlin.Result<java.lang.Integer>)

  public final void setClassPropInResultExtension(@org.jetbrains.annotations.NotNull() java.lang.Object, int);//  setClassPropInResultExtension(java.lang.Object, int)

  public final void setClassResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object);//  setClassResultProp(java.lang.Object)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Object getInterfaceResultProp-d1pmJ48();//  getInterfaceResultProp-d1pmJ48()

  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.Object interfaceResultInReturn-d1pmJ48();//  interfaceResultInReturn-d1pmJ48()

  public abstract void interfaceResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object);//  interfaceResultInParameter(java.lang.Object)

  public abstract void setInterfaceResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object);//  setInterfaceResultProp(java.lang.Object)
}

public final class ResultInSignatureKt /* ResultInSignatureKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static java.lang.Object topLevelResultProp;

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.Object getTopLevelResultProp();//  getTopLevelResultProp()

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.Object topLevelResultInReturn();//  topLevelResultInReturn()

  public static final void setTopLevelResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object);//  setTopLevelResultProp(java.lang.Object)

  public static final void topLevelResultInContext(@org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInContext(java.lang.Object)

  public static final void topLevelResultInExtension(@org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInExtension(java.lang.Object)

  public static final void topLevelResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInParameter(java.lang.Object)
}

@kotlin.jvm.JvmInline()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String constructor-impl(@org.jetbrains.annotations.NotNull() java.lang.String);//  constructor-impl(java.lang.String)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.String, java.lang.Object);//  equals-impl(java.lang.String, java.lang.Object)

  public static final boolean equals-impl0(java.lang.String, java.lang.String);//  equals-impl0(java.lang.String, java.lang.String)

  public static int hashCode-impl(java.lang.String);//  hashCode-impl(java.lang.String)

  public static java.lang.String toString-impl(java.lang.String);//  toString-impl(java.lang.String)
}

@kotlin.jvm.JvmInline()
public final class ValueClassWithResult /* ValueClassWithResult*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.Object r;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object getR-d1pmJ48();//  getR-d1pmJ48()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.Object constructor-impl(@org.jetbrains.annotations.NotNull() java.lang.Object);//  constructor-impl(java.lang.Object)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.Object, java.lang.Object);//  equals-impl(java.lang.Object, java.lang.Object)

  public static final boolean equals-impl0(java.lang.Object, java.lang.Object);//  equals-impl0(java.lang.Object, java.lang.Object)

  public static final void funInValueClass-impl(java.lang.Object);//  funInValueClass-impl(java.lang.Object)

  public static final void funWithResultParameter-impl(java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.Object);//  funWithResultParameter-impl(java.lang.Object, java.lang.Object)

  public static int hashCode-impl(java.lang.Object);//  hashCode-impl(java.lang.Object)

  public static java.lang.String toString-impl(java.lang.Object);//  toString-impl(java.lang.Object)
}
