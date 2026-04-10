@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract SimpleAnn[] t();//  t()

  public abstract int x();//  x()

  public abstract java.lang.Class<?> z();//  z()

  public abstract java.lang.Class<?>[] e();//  e()

  public abstract java.lang.String y();//  y()

  public abstract kotlin.DeprecationLevel depr();//  depr()
}

public abstract interface Base /* Base*/ {
  @Ann(t = {})
  public abstract void foo(@Ann(t = {}) @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class Derived /* Derived*/ implements Base {
  @Ann(t = {})
  @java.lang.Override()
  public void foo(@Ann(t = {}) @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  Derived(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Base);//  .ctor(@org.jetbrains.annotations.NotNull() Base)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface SimpleAnn /* SimpleAnn*/ {
  public abstract java.lang.String value();//  value()
}
