public final class ClassWithResultConstructor /* ClassWithResultConstructor*/ {
  private  ClassWithResultConstructor(@org.jetbrains.annotations.NotNull() java.lang.Object);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.Object)
}

public final class ClassWithValueClassConstructor /* ClassWithValueClassConstructor*/ {
  private  ClassWithValueClassConstructor(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.Object classResultProp;

  @org.jetbrains.annotations.Nullable()
  private @org.jetbrains.annotations.Nullable() kotlin.Result<@org.jetbrains.annotations.NotNull() java.lang.Integer> classNullableResultProp = null /* initializer type: null */;

  @kotlin.jvm.JvmName(name = "resultInReturnWithJvmName")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.Object resultInReturnWithJvmName();//  resultInReturnWithJvmName()

  public  RegularClass();//  .ctor()
}

public abstract interface RegularInterface /* RegularInterface*/ {
}

public final class ResultInSignatureKt /* ResultInSignatureKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.Object topLevelResultProp;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.Object getTopLevelResultProp();//  getTopLevelResultProp()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.Object topLevelResultInReturn();//  topLevelResultInReturn()
}

@kotlin.jvm.JvmInline()
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

@kotlin.jvm.JvmInline()
public final class ValueClassWithResult /* ValueClassWithResult*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.Object r;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
