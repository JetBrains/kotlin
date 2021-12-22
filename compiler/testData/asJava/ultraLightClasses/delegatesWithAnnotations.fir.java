public abstract @interface SimpleAnn /* SimpleAnn*/ {
  public abstract java.lang.String value();//  value()

}

public abstract @interface Ann /* Ann*/ {
  public abstract SimpleAnn[] t();//  t()

  public abstract int x();//  x()

  public abstract java.lang.Class<?> z();//  z()

  public abstract java.lang.Class<?>[] e();//  e()

  public abstract java.lang.String y();//  y()

  public abstract kotlin.DeprecationLevel depr();//  depr()

}

public abstract interface Base /* Base*/ {
  @Ann(x = 1L, y = "134", z = java.lang.String.class, e = ?, depr = kotlin.DeprecationLevel.WARNING, t = ?)
  public abstract void foo(@Ann(x = 2L, y = "324", z = Ann.class, e = ?, depr = kotlin.DeprecationLevel.WARNING, t = ?) @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(java.lang.String)

}

public final class Derived /* Derived*/ implements Base {
  @Ann(x = 1L, y = "134", z = java.lang.String.class, e = ?, depr = kotlin.DeprecationLevel.WARNING, t = ?)
  public void foo(@Ann(x = 2L, y = "324", z = Ann.class, e = ?, depr = kotlin.DeprecationLevel.WARNING, t = ?) @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(java.lang.String)

  public  Derived(@org.jetbrains.annotations.NotNull() Base);//  .ctor(Base)

}
