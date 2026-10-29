@kotlin.jvm.JvmInline()
public final class Arr /* test.Arr*/<T extends java.lang.CharSequence>  {
  @org.jetbrains.annotations.NotNull()
  private final T[] a;

  @org.jetbrains.annotations.NotNull()
  public final T[] getA();//  getA()

  @org.jetbrains.annotations.NotNull()
  public static <T extends java.lang.CharSequence> java.lang.CharSequence[] constructor-impl(@org.jetbrains.annotations.NotNull() T[]);// <T extends java.lang.CharSequence>  constructor-impl(T[])

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.CharSequence[], java.lang.Object);//  equals-impl(java.lang.CharSequence[], java.lang.Object)

  public static final boolean equals-impl0(java.lang.CharSequence[], java.lang.CharSequence[]);//  equals-impl0(java.lang.CharSequence[], java.lang.CharSequence[])

  public static int hashCode-impl(java.lang.CharSequence[]);//  hashCode-impl(java.lang.CharSequence[])

  public static java.lang.String toString-impl(java.lang.CharSequence[]);//  toString-impl(java.lang.CharSequence[])
}

@kotlin.jvm.JvmInline()
public final class AsList /* test.AsList*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final java.util.List<T> x;

  @org.jetbrains.annotations.NotNull()
  public final java.util.List<T> getX();//  getX()

  @org.jetbrains.annotations.NotNull()
  public static <T> java.util.List<T> constructor-impl(@org.jetbrains.annotations.NotNull() java.util.List<? extends T>);// <T>  constructor-impl(java.util.List<? extends T>)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.util.List<? extends T>, java.lang.Object);//  equals-impl(java.util.List<? extends T>, java.lang.Object)

  public static final boolean equals-impl0(java.util.List<? extends java.lang.Object>, java.util.List<? extends java.lang.Object>);//  equals-impl0(java.util.List<? extends java.lang.Object>, java.util.List<? extends java.lang.Object>)

  public static int hashCode-impl(java.util.List<? extends T>);//  hashCode-impl(java.util.List<? extends T>)

  public static java.lang.String toString-impl(java.util.List<? extends T>);//  toString-impl(java.util.List<? extends T>)
}

public final class Holder /* test.Holder*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.CharSequence[] arr;

  @org.jetbrains.annotations.NotNull()
  private final java.util.List<java.lang.Integer> list;

  @org.jetbrains.annotations.NotNull()
  private final java.util.List<java.util.List<java.lang.Integer>> ctorParam;

  @org.jetbrains.annotations.Nullable()
  private final test.Wrapper<java.lang.String> nullable;

  @kotlin.jvm.JvmName(name = "generic")
  public final <R> void generic(@org.jetbrains.annotations.NotNull() java.util.List<? extends R>);// <R>  generic(java.util.List<? extends R>)

  @kotlin.jvm.JvmName(name = "give")
  @org.jetbrains.annotations.Nullable()
  public final test.Wrapper<java.lang.Integer> give();//  give()

  @kotlin.jvm.JvmName(name = "takeList")
  public final void takeList(@org.jetbrains.annotations.NotNull() java.util.List<? extends java.util.List<java.lang.Integer>>);//  takeList(java.util.List<? extends java.util.List<java.lang.Integer>>)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.CharSequence[] getArr-mZM3UDs();//  getArr-mZM3UDs()

  @org.jetbrains.annotations.NotNull()
  public final java.util.List<java.lang.Integer> getList-gDuY8Gg();//  getList-gDuY8Gg()

  @org.jetbrains.annotations.NotNull()
  public final java.util.List<java.util.List<java.lang.Integer>> getCtorParam-gDuY8Gg();//  getCtorParam-gDuY8Gg()

  @org.jetbrains.annotations.Nullable()
  public final test.Wrapper<java.lang.String> getNullable-9UDvz2U();//  getNullable-9UDvz2U()

  private  Holder(java.util.List<? extends java.util.List<java.lang.Integer>>);//  .ctor(java.util.List<? extends java.util.List<java.lang.Integer>>)
}

@kotlin.jvm.JvmInline()
public final class WithWrapper /* test.WithWrapper*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final test.Wrapper<T> x;

  @org.jetbrains.annotations.NotNull()
  public final test.Wrapper<T> getX();//  getX()

  @org.jetbrains.annotations.NotNull()
  public static <T> test.Wrapper<T> constructor-impl(@org.jetbrains.annotations.NotNull() test.Wrapper<T>);// <T>  constructor-impl(test.Wrapper<T>)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(test.Wrapper<T>, java.lang.Object);//  equals-impl(test.Wrapper<T>, java.lang.Object)

  public static final boolean equals-impl0(test.Wrapper<java.lang.Object>, test.Wrapper<java.lang.Object>);//  equals-impl0(test.Wrapper<java.lang.Object>, test.Wrapper<java.lang.Object>)

  public static int hashCode-impl(test.Wrapper<T>);//  hashCode-impl(test.Wrapper<T>)

  public static java.lang.String toString-impl(test.Wrapper<T>);//  toString-impl(test.Wrapper<T>)
}

public final class Wrapper /* test.Wrapper*/<T>  {
  private final T v;

  public  Wrapper(T);//  .ctor(T)

  public final T getV();//  getV()
}
