@Unsigned(count = 1, more = {})
public final class Annotated /* Annotated*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String wrapper;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final StringWrapper getWrapper();//  getWrapper()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Annotated(@org.jetbrains.annotations.NotNull() StringWrapper);//  .ctor(StringWrapper)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getWrapper-K4fyztM();//  getWrapper-K4fyztM()

  private  Annotated(java.lang.String);//  .ctor(java.lang.String)
}

@kotlin.jvm.JvmInline()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String s;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  StringWrapper(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getS();//  getS()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String constructor-impl(@org.jetbrains.annotations.NotNull() java.lang.String);//  constructor-impl(java.lang.String)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.String, java.lang.Object);//  equals-impl(java.lang.String, java.lang.Object)

  public static final boolean equals-impl0(java.lang.String, java.lang.String);//  equals-impl0(java.lang.String, java.lang.String)

  public static int hashCode-impl(java.lang.String);//  hashCode-impl(java.lang.String)

  public static java.lang.String toString-impl(java.lang.String);//  toString-impl(java.lang.String)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Unsigned /* Unsigned*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public abstract kotlin.UInt getCount();//  getCount()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public abstract kotlin.UIntArray getMore();//  getMore()

  public abstract int count();//  count()

  public abstract int[] more();//  more()
}
