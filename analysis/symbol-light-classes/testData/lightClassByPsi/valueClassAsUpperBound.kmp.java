public final class RegularClass /* RegularClass*/ {
  @<error>()
  public final <T extends Some> void classFunWithJvmName(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunWithJvmName(@org.jetbrains.annotations.NotNull() T)

  @org.jetbrains.annotations.NotNull()
  public final <T extends Some> @org.jetbrains.annotations.NotNull() T classFunInReturn();// <T extends Some>  classFunInReturn()

  public  RegularClass();//  .ctor()

  public final <T extends Some> int getClassPropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  getClassPropInExtension(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void classFunInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInExtension(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void classFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInParameter(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void setClassPropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T, int);// <T extends Some>  setClassPropInExtension(@org.jetbrains.annotations.NotNull() T, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract <T extends Some> @org.jetbrains.annotations.NotNull() T interfaceFunInReturn();// <T extends Some>  interfaceFunInReturn()

  public abstract <T extends Some> void interfaceFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  interfaceFunInParameter(@org.jetbrains.annotations.NotNull() T)
}

public final class ResultAsUpperBound /* ResultAsUpperBound*/ {
  @org.jetbrains.annotations.NotNull()
  public final <T extends kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String>> @org.jetbrains.annotations.NotNull() T funInReturn();// <T extends kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String>>  funInReturn()

  public  ResultAsUpperBound();//  .ctor()

  public final <T extends kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String>> void funInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String>>  funInParameter(@org.jetbrains.annotations.NotNull() T)
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

public final class ValueClassAsUpperBoundKt /* ValueClassAsUpperBoundKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final <T extends Some> @org.jetbrains.annotations.NotNull() T topLevelFunInReturn();// <T extends Some>  topLevelFunInReturn()

  public static final <T extends Some> void topLevelFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  topLevelFunInParameter(@org.jetbrains.annotations.NotNull() T)
}
