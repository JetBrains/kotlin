@kotlin.jvm.JvmInline()
public final class Arr /* test.Arr*/<T extends java.lang.CharSequence>  {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [] a;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() T @org.jetbrains.annotations.NotNull() [] getA();//  getA()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

@kotlin.jvm.JvmInline()
public final class AsList /* test.AsList*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() T> x;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() T> getX();//  getX()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class Holder /* test.Holder*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.CharSequence @org.jetbrains.annotations.NotNull() [] arr;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer> list;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>> ctorParam;

  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() test.Wrapper<@org.jetbrains.annotations.NotNull() java.lang.String> nullable = null /* initializer type: null */;

  @kotlin.jvm.JvmName(name = "generic")
  public final <R> void generic(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() R>);// <R>  generic(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() R>)

  @kotlin.jvm.JvmName(name = "give")
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() test.Wrapper<@org.jetbrains.annotations.NotNull() java.lang.Integer> give();//  give()

  @kotlin.jvm.JvmName(name = "takeList")
  public final void takeList(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>>);//  takeList(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>>)

  private  Holder(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>>);//  .ctor(@org.jetbrains.annotations.NotNull() java.util.List<? extends @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>>)
}

@kotlin.jvm.JvmInline()
public final class WithWrapper /* test.WithWrapper*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() test.Wrapper<@org.jetbrains.annotations.NotNull() T> x;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() test.Wrapper<@org.jetbrains.annotations.NotNull() T> getX();//  getX()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class Wrapper /* test.Wrapper*/<T>  {
  private final T v;

  public  Wrapper(T);//  .ctor(T)

  public final T getV();//  getV()
}
