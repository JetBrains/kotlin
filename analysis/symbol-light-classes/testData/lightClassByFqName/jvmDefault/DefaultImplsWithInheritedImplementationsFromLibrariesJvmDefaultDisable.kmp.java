public abstract interface B /* B*/ extends Disabled, Enabled, NoCompatibility, kotlin.coroutines.CoroutineContext.Element, kotlin.ranges.ClosedRange<@org.jetbrains.annotations.NotNull() java.lang.Integer> {
  public abstract void declared();//  declared()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext minusKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<?>);//  minusKey(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<?>)

    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext plus(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext);//  plus(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext)

    @org.jetbrains.annotations.Nullable()
    public static <E extends kotlin.coroutines.CoroutineContext.Element> @org.jetbrains.annotations.Nullable() E get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<@org.jetbrains.annotations.NotNull() E>);// <E extends kotlin.coroutines.CoroutineContext.Element>  get(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<@org.jetbrains.annotations.NotNull() E>)

    public static <R> R fold(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, R, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function2<? super R, ? super @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Element, ? extends R>);// <R>  fold(@org.jetbrains.annotations.NotNull() B, R, @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function2<? super R, ? super @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Element, ? extends R>)

    public static boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, int);//  contains(@org.jetbrains.annotations.NotNull() B, int)

    public static boolean isEmpty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  isEmpty(@org.jetbrains.annotations.NotNull() B)

    public static void declared(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  declared(@org.jetbrains.annotations.NotNull() B)

    public static void fromDisabled(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  fromDisabled(@org.jetbrains.annotations.NotNull() B)

    public static void fromEnabled(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  fromEnabled(@org.jetbrains.annotations.NotNull() B)

    public static void fromNoCompatibility(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  fromNoCompatibility(@org.jetbrains.annotations.NotNull() B)
  }
}
