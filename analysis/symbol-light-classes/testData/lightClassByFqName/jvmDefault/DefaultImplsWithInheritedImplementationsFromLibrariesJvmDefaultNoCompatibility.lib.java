public abstract interface B /* B*/ extends Disabled, Enabled, NoCompatibility, kotlin.coroutines.CoroutineContext.Element, kotlin.ranges.ClosedRange<java.lang.Integer> {
  public default void declared();//  declared()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static kotlin.coroutines.CoroutineContext minusKey(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<?>);//  minusKey(B, kotlin.coroutines.CoroutineContext.Key<?>)

    @org.jetbrains.annotations.NotNull()
    public static kotlin.coroutines.CoroutineContext plus(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext);//  plus(B, kotlin.coroutines.CoroutineContext)

    @org.jetbrains.annotations.Nullable()
    public static <E extends kotlin.coroutines.CoroutineContext.Element> E get(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<E>);// <E extends kotlin.coroutines.CoroutineContext.Element>  get(B, kotlin.coroutines.CoroutineContext.Key<E>)

    public static <R> R fold(@org.jetbrains.annotations.NotNull() B, R, @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function2<? super R, ? super kotlin.coroutines.CoroutineContext.Element, ? extends R>);// <R>  fold(B, R, kotlin.jvm.functions.Function2<? super R, ? super kotlin.coroutines.CoroutineContext.Element, ? extends R>)

    public static boolean contains(@org.jetbrains.annotations.NotNull() B, int);//  contains(B, int)

    public static boolean isEmpty(@org.jetbrains.annotations.NotNull() B);//  isEmpty(B)

    public static void fromDisabled(@org.jetbrains.annotations.NotNull() B);//  fromDisabled(B)
  }
}
