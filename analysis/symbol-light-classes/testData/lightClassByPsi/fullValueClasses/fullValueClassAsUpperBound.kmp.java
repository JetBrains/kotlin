public abstract class AbstractSome /* AbstractSome*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public  AbstractSome();//  .ctor()
}

public final class FullValueClassAsUpperBoundKt /* FullValueClassAsUpperBoundKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final <T extends Some> @org.jetbrains.annotations.NotNull() T topLevelFunInReturn();// <T extends Some>  topLevelFunInReturn()

  public static final <T extends Some> void topLevelFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  topLevelFunInParameter(@org.jetbrains.annotations.NotNull() T)
}

public final class RegularClass /* RegularClass*/ {
  @org.jetbrains.annotations.NotNull()
  public final <T extends AbstractSome> @org.jetbrains.annotations.NotNull() T abstractInReturn();// <T extends AbstractSome>  abstractInReturn()

  @org.jetbrains.annotations.NotNull()
  public final <T extends Some> @org.jetbrains.annotations.NotNull() T classFunInReturn();// <T extends Some>  classFunInReturn()

  public  RegularClass();//  .ctor()

  public final <T extends AbstractSome> void abstractInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends AbstractSome>  abstractInParameter(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> int getClassPropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  getClassPropInExtension(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void classFunInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInExtension(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void classFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInParameter(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void classFunWithJvmName(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunWithJvmName(@org.jetbrains.annotations.NotNull() T)

  public final <T extends Some> void setClassPropInExtension(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T, int);// <T extends Some>  setClassPropInExtension(@org.jetbrains.annotations.NotNull() T, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract <T extends Some> @org.jetbrains.annotations.NotNull() T interfaceFunInReturn();// <T extends Some>  interfaceFunInReturn()

  public abstract <T extends Some> void interfaceFunInParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() T);// <T extends Some>  interfaceFunInParameter(@org.jetbrains.annotations.NotNull() T)
}

public final class Some /* Some*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  private final int count;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public  Some(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getCount();//  getCount()

  public int hashCode();//  hashCode()
}
