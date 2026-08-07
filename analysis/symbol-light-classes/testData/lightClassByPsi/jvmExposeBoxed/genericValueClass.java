@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmInline()
public final class Box /* Box*/<T>  {
  private final T value;

  @kotlin.jvm.JvmExposeBoxed()
  public  Box(T);//  .ctor(T)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final T getValue();//  getValue()

  public int hashCode();//  hashCode()
}

public final class GenericValueClassKt /* GenericValueClassKt*/ {
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final <T> @org.jetbrains.annotations.NotNull() Box<T> roundTrip(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Box<T>);// <T>  roundTrip(@org.jetbrains.annotations.NotNull() Box<T>)
}
