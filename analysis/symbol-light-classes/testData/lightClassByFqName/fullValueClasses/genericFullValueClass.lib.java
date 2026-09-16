public final class Pair /* pack.Pair*/<A, B>  {
  @org.jetbrains.annotations.Nullable()
  private final B second;

  private final A first;

  @org.jetbrains.annotations.NotNull()
  public final <C> pack.Pair<C, B> map(@org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function1<? super A, ? extends C>);// <C>  map(kotlin.jvm.functions.Function1<? super A, ? extends C>)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  @org.jetbrains.annotations.Nullable()
  public final B getSecond();//  getSecond()

  public  Pair(A, @org.jetbrains.annotations.Nullable() B);//  .ctor(A, B)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final A getFirst();//  getFirst()

  public int hashCode();//  hashCode()
}
