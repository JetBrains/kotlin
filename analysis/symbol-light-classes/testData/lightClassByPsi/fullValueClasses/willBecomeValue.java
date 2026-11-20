@kotlin.OptIn(markerClass = {kotlin.ExperimentalValueClassesApi.class})
@kotlin.WillBecomeValue()
public final class Money /* pack.Money*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String currency;

  private final long amount;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @java.lang.Override()
  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  @java.lang.Override()
  public int hashCode();//  hashCode()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getCurrency();//  getCurrency()

  public  Money(long, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(long, @org.jetbrains.annotations.NotNull() java.lang.String)

  public final long getAmount();//  getAmount()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalValueClassesApi.class})
@kotlin.WillBecomeValue()
public final class NoMoney /* pack.NoMoney*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.NoMoney INSTANCE;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  NoMoney();//  .ctor()
}
