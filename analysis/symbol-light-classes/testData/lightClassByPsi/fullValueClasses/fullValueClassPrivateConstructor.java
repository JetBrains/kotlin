public final class ThreadLocalDelegate /* pack.ThreadLocalDelegate*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String name;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.ThreadLocal<T> threadLocal;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() pack.ThreadLocalDelegate.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  ThreadLocalDelegate(@org.jetbrains.annotations.NotNull() java.lang.ThreadLocal<T>, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.ThreadLocal<T>, @org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* pack.ThreadLocalDelegate.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  public final <T> @org.jetbrains.annotations.NotNull() pack.ThreadLocalDelegate<T> create(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);// <T>  create(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  Companion();//  .ctor()
}
