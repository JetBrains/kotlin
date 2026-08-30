public enum Enum /* Enum*/ {
  FIRST {
   FIRST();//  .ctor()
  },
  SECOND;

  @kotlin.jvm.JvmExposeBoxed()
  public final void finalFunction(@org.jetbrains.annotations.NotNull() ValueInt);//  finalFunction(@org.jetbrains.annotations.NotNull() ValueInt)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Enum @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Enum valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() Enum> getEntries();//  getEntries()

  private  Enum();//  .ctor()
}

static final class FIRST /* Enum.FIRST*/ extends Enum {
   FIRST();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class ValueInt /* ValueInt*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueInt(int);//  .ctor(int)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}
