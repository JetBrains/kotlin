public final class RegularClass /* RegularClass*/ {
  @kotlin.jvm.JvmName(name = "specialName")
  public final <T extends Some> void specialName(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  specialName(T)

  @org.jetbrains.annotations.NotNull()
  public final <T extends Some> T classFunInReturn-YO-7n-0();// <T extends Some>  classFunInReturn-YO-7n-0()

  public  RegularClass();//  .ctor()

  public final <T extends Some> int getClassPropInExtension-5lyY9Q4(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  getClassPropInExtension-5lyY9Q4(T)

  public final <T extends Some> void classFunInExtension-5lyY9Q4(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInExtension-5lyY9Q4(T)

  public final <T extends Some> void classFunInParameter-5lyY9Q4(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  classFunInParameter-5lyY9Q4(T)

  public final <T extends Some> void setClassPropInExtension-54afNMI(@org.jetbrains.annotations.NotNull() T, int);// <T extends Some>  setClassPropInExtension-54afNMI(T, int)
}

public abstract interface RegularInterface /* RegularInterface*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract <T extends Some> T interfaceFunInReturn-YO-7n-0();// <T extends Some>  interfaceFunInReturn-YO-7n-0()

  public abstract <T extends Some> void interfaceFunInParameter-5lyY9Q4(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  interfaceFunInParameter-5lyY9Q4(T)
}

public final class ResultAsUpperBound /* ResultAsUpperBound*/ {
  @org.jetbrains.annotations.NotNull()
  public final <T extends kotlin.Result<? extends java.lang.String>> T funInReturn-d1pmJ48();// <T extends kotlin.Result<? extends java.lang.String>>  funInReturn-d1pmJ48()

  public  ResultAsUpperBound();//  .ctor()

  public final <T extends kotlin.Result<? extends java.lang.String>> void funInParameter(@org.jetbrains.annotations.NotNull() T);// <T extends kotlin.Result<? extends java.lang.String>>  funInParameter(T)
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

public final class ValueClassAsUpperBoundKt /* ValueClassAsUpperBoundKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final <T extends Some> T topLevelFunInReturn();// <T extends Some>  topLevelFunInReturn()

  public static final <T extends Some> void topLevelFunInParameter-5lyY9Q4(@org.jetbrains.annotations.NotNull() T);// <T extends Some>  topLevelFunInParameter-5lyY9Q4(T)
}
