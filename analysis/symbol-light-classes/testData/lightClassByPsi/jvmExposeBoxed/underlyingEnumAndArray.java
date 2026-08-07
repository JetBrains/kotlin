public enum Color /* Color*/ {
  RED;

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Color @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Color valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() Color> getEntries();//  getEntries()

  private  Color();//  .ctor()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmInline()
public final class ColorBox /* ColorBox*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() Color color;

  @kotlin.jvm.JvmExposeBoxed()
  public  ColorBox(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Color);//  .ctor(@org.jetbrains.annotations.NotNull() Color)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Color getColor();//  getColor()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmInline()
public final class IntArrayBox /* IntArrayBox*/ {
  @org.jetbrains.annotations.NotNull()
  private final int @org.jetbrains.annotations.NotNull() [] array;

  @kotlin.jvm.JvmExposeBoxed()
  public  IntArrayBox(@org.jetbrains.annotations.NotNull() int @org.jetbrains.annotations.NotNull() []);//  .ctor(int @org.jetbrains.annotations.NotNull() [])

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final int @org.jetbrains.annotations.NotNull() [] getArray();//  getArray()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class UnderlyingEnumAndArrayKt /* UnderlyingEnumAndArrayKt*/ {
  @kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String nameOf(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() ColorBox);//  nameOf(@org.jetbrains.annotations.NotNull() ColorBox)
}
