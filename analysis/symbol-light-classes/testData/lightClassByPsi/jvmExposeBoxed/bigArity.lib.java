public abstract interface BigArity /* BigArity*/<T>  {
  @org.jetbrains.annotations.NotNull()
  public abstract java.lang.String invoke(@org.jetbrains.annotations.NotNull() java.lang.String, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T);//  invoke(java.lang.String, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T, T)
}

public final class Child /* Child*/ implements BigArity<Id> {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public java.lang.String invoke(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id, @org.jetbrains.annotations.NotNull() Id);//  invoke(java.lang.String, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id, Id)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String invoke-t09OwRc(@org.jetbrains.annotations.NotNull() java.lang.String, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long);//  invoke-t09OwRc(java.lang.String, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long, long)

  public  Child();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class Id /* Id*/ {
  private final long value;

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public final long getValue();//  getValue()

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(long, java.lang.Object);//  equals-impl(long, java.lang.Object)

  public static final boolean equals-impl0(long, long);//  equals-impl0(long, long)

  public static int hashCode-impl(long);//  hashCode-impl(long)

  public static java.lang.String toString-impl(long);//  toString-impl(long)

  public static long constructor-impl(long);//  constructor-impl(long)
}
