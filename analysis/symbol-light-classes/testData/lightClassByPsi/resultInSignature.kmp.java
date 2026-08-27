public final class ClassWithResultConstructor /* ClassWithResultConstructor*/ {
  public  ClassWithResultConstructor(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public final class ClassWithValueClassConstructor /* ClassWithValueClassConstructor*/ {
  public  ClassWithValueClassConstructor(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.Object classResultProp;

  @org.jetbrains.annotations.Nullable()
  private @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer> classNullableResultProp = null /* initializer type: null */;

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object classResultInReturnWithJvmName();//  classResultInReturnWithJvmName()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object classResultInReturn();//  classResultInReturn()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object getClassResultProp();//  getClassResultProp()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() java.lang.Object classSuspendResultInReturn(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer>>);//  classSuspendResultInReturn(@org.jetbrains.annotations.NotNull() kotlin.coroutines.Continuation<? super @org.jetbrains.annotations.NotNull() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer>>)

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer> classNullableResultInReturn();//  classNullableResultInReturn()

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer> getClassNullableResultProp();//  getClassNullableResultProp()

  public  RegularClass();//  .ctor()

  public final int getClassPropInResultExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  getClassPropInResultExtension(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void classResultAndValueClassInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  classResultAndValueClassInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object, @org.jetbrains.annotations.NotNull() java.lang.String)

  public final void classResultInContext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInContext(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void classResultInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInExtension(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void classResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void setClassNullableResultProp(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  setClassNullableResultProp(@org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public final void setClassPropInResultExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object, int);//  setClassPropInResultExtension(@org.jetbrains.annotations.NotNull() java.lang.Object, int)

  public final void setClassResultProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  setClassResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Object getInterfaceResultProp();//  getInterfaceResultProp()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Object interfaceResultInReturn();//  interfaceResultInReturn()

  public abstract void interfaceResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  interfaceResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public abstract void setInterfaceResultProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  setInterfaceResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public final class ResultInSignatureKt /* ResultInSignatureKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.Object topLevelResultProp;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.Object getTopLevelResultProp();//  getTopLevelResultProp()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.Object topLevelResultInReturn();//  topLevelResultInReturn()

  public static final void setTopLevelResultProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  setTopLevelResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public static final void topLevelResultInContext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInContext(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public static final void topLevelResultInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInExtension(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public static final void topLevelResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  topLevelResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

@<error>()
public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()
}

@<error>()
public final class ValueClassWithResult /* ValueClassWithResult*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.Object r;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object getR();//  getR()
}
