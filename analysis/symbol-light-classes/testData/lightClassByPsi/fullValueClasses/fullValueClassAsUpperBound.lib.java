public abstract class AbstractSome /* AbstractSome*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String getValue();//  getValue()

  public  AbstractSome();//  .ctor()
}

public final class FullValueClassAsUpperBoundKt /* FullValueClassAsUpperBoundKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final <T extends Some> T topLevelFunInReturn();// <T extends Some>  topLevelFunInReturn()

  public static final <T extends Some> void topLevelFunInParameter(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  topLevelFunInParameter(T)
}

public final class RegularClass /* RegularClass*/ {
  @kotlin.jvm.JvmName(name = "specialName")
  public final <T extends Some> void specialName(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  specialName(T)

  @org.jetbrains.annotations.NotNull()
  public final <T extends AbstractSome> T abstractInReturn();// <T extends AbstractSome>  abstractInReturn()

  @org.jetbrains.annotations.NotNull()
  public final <T extends Some> T classFunInReturn();// <T extends Some>  classFunInReturn()

  public  RegularClass();//  .ctor()

  public final <T extends AbstractSome> void abstractInParameter(@org.jetbrains.annotations.NotNull() T);// <T extends AbstractSome>  abstractInParameter(T)

  public final <T extends Some> int getClassPropInExtension(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  getClassPropInExtension(T)

  public final <T extends Some> void classFunInExtension(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInExtension(T)

  public final <T extends Some> void classFunInParameter(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInParameter(T)

  public final <T extends Some> void setClassPropInExtension(@org.jetbrains.annotations.NotNull() T, int);// <T extends Some>  setClassPropInExtension(T, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract <T extends Some> T interfaceFunInReturn();// <T extends Some>  interfaceFunInReturn()

  public abstract <T extends Some> void interfaceFunInParameter(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  interfaceFunInParameter(T)
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
