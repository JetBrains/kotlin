public final class RegularClass /* RegularClass*/ {
  public  RegularClass();//  .ctor()
}

public abstract interface RegularInterface /* RegularInterface*/ {
}

public final class ResultAsUpperBound /* ResultAsUpperBound*/ {
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
}
