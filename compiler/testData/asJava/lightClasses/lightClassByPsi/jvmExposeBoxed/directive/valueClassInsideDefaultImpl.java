public abstract interface BaseInterface /* one.BaseInterface*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.Nullable() one.MyValueClass getPropertyWithValueClassParameter();//  getPropertyWithValueClassParameter()

  @kotlin.jvm.JvmExposeBoxed()
  public abstract void functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() one.MyValueClass);//  functionWithValueClassParameter(@org.jetbrains.annotations.NotNull() one.MyValueClass)

  public abstract void regularFunction();//  regularFunction()

  class DefaultImpls ...
}

@kotlin.jvm.JvmInline()
public final class MyValueClass /* one.MyValueClass*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String str;

  @kotlin.jvm.JvmExposeBoxed()
  public  MyValueClass(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getStr();//  getStr()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}
