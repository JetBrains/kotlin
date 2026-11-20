@kotlin.WillBecomeValue()
public final class Money /* pack.Money*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String currency;

  private final long amount;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getCurrency();//  getCurrency()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  Money(long, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(long, java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final long getAmount();//  getAmount()

  public int hashCode();//  hashCode()
}

@kotlin.WillBecomeValue()
public final class NoMoney /* pack.NoMoney*/ {
  @org.jetbrains.annotations.NotNull()
  public static final pack.NoMoney INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  NoMoney();//  .ctor()
}
