public final class ClassWithResultConstructor /* ClassWithResultConstructor*/ {
  public  ClassWithResultConstructor(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public final class ClassWithValueClassConstructor /* ClassWithValueClassConstructor*/ {
  private  ClassWithValueClassConstructor(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.Object classResultProp;

  @org.jetbrains.annotations.Nullable()
  private @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer> classNullableResultProp = null /* initializer type: null */;

  public  RegularClass();//  .ctor()

  public final int getClassPropInResultExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  getClassPropInResultExtension(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void classResultInContext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInContext(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void classResultInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInExtension(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void classResultInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  classResultInParameter(@org.jetbrains.annotations.NotNull() java.lang.Object)

  public final void setClassNullableResultProp(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  setClassNullableResultProp(@org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  public final void setClassPropInResultExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object, int);//  setClassPropInResultExtension(@org.jetbrains.annotations.NotNull() java.lang.Object, int)

  public final void setClassResultProp(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  setClassResultProp(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public abstract interface RegularInterface /* RegularInterface*/ {
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
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

@<error>()
public final class ValueClassWithResult /* ValueClassWithResult*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.Object r;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
