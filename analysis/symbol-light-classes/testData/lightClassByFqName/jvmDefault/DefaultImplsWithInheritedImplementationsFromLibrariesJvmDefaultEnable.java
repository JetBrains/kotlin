public abstract interface B /* B*/ extends Disabled, Enabled, NoCompatibility, java.lang.Cloneable, kotlin.coroutines.CoroutineContext.Element, kotlin.ranges.ClosedRange<@org.jetbrains.annotations.NotNull() java.lang.Integer> {
  public default void declared();//  declared()

  public static final class DefaultImpls /* B.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() java.lang.Object clone(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  clone(@org.jetbrains.annotations.NotNull() B)

    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext minusKey(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<?>);//  minusKey(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<?>)

    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext plus(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext);//  plus(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext)

    @java.lang.Deprecated()
    @org.jetbrains.annotations.Nullable()
    public static <E extends kotlin.coroutines.CoroutineContext.Element> @org.jetbrains.annotations.Nullable() E get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<@org.jetbrains.annotations.NotNull() E>);// <E extends kotlin.coroutines.CoroutineContext.Element>  get(@org.jetbrains.annotations.NotNull() B, @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Key<@org.jetbrains.annotations.NotNull() E>)

    @java.lang.Deprecated()
    public static <R> R fold(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, R, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function2<? super R, ? super @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Element, ? extends R>);// <R>  fold(@org.jetbrains.annotations.NotNull() B, R, @org.jetbrains.annotations.NotNull() kotlin.jvm.functions.Function2<? super R, ? super @org.jetbrains.annotations.NotNull() kotlin.coroutines.CoroutineContext.Element, ? extends R>)

    @java.lang.Deprecated()
    public static boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B, int);//  contains(@org.jetbrains.annotations.NotNull() B, int)

    @java.lang.Deprecated()
    public static boolean isEmpty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  isEmpty(@org.jetbrains.annotations.NotNull() B)

    @java.lang.Deprecated()
    public static void declared(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  declared(@org.jetbrains.annotations.NotNull() B)

    @java.lang.Deprecated()
    public static void fromDisabled(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  fromDisabled(@org.jetbrains.annotations.NotNull() B)

    @java.lang.Deprecated()
    public static void fromEnabled(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  fromEnabled(@org.jetbrains.annotations.NotNull() B)

    @java.lang.Deprecated()
    public static void fromNoCompatibility(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() B);//  fromNoCompatibility(@org.jetbrains.annotations.NotNull() B)
  }
}
