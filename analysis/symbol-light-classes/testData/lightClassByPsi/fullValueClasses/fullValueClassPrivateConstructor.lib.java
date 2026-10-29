public final class ThreadLocalDelegate /* pack.ThreadLocalDelegate*/<T>  {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String name;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.ThreadLocal<T> threadLocal;

  @org.jetbrains.annotations.NotNull()
  public static final pack.ThreadLocalDelegate.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  ThreadLocalDelegate(java.lang.ThreadLocal<T>, java.lang.String);//  .ctor(java.lang.ThreadLocal<T>, java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* pack.ThreadLocalDelegate.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  public final <T> pack.ThreadLocalDelegate<T> create(@org.jetbrains.annotations.NotNull() java.lang.String);// <T>  create(java.lang.String)

  private  Companion();//  .ctor()
}
