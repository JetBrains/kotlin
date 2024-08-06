@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() SimpleAnn @org.jetbrains.annotations.NotNull() [] t();//  t()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<?> @org.jetbrains.annotations.NotNull() [] e();//  e()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<?> z();//  z()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String y();//  y()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() kotlin.DeprecationLevel depr();//  depr()

  public abstract int x();//  x()
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
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String value();//  value()
}
