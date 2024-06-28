@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  public abstract @org.jetbrains.annotations.NotNull() SimpleAnn @org.jetbrains.annotations.NotNull() [] t();//  t()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<?> @org.jetbrains.annotations.NotNull() [] e();//  e()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.Class<?> z();//  z()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String y();//  y()

  public abstract @org.jetbrains.annotations.NotNull() kotlin.DeprecationLevel depr();//  depr()

  public abstract int x();//  x()
}

public abstract interface Base /* Base*/ {
  @Ann(x = 1, y = "134", z = java.lang.String.class, e = {int.class, double.class}, depr = kotlin.DeprecationLevel.WARNING, t = {SimpleAnn("243"), SimpleAnn("4324")})
  public abstract void foo(@Ann(x = 2, y = "324", z = Ann.class, e = {byte.class, Base.class}, depr = kotlin.DeprecationLevel.WARNING, t = {SimpleAnn("687"), SimpleAnn("78")}) @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class Derived /* Derived*/ implements Base {
  @Ann(x = 1, y = "134", z = java.lang.String.class, e = {int.class, double.class}, depr = kotlin.DeprecationLevel.WARNING, t = {SimpleAnn("243"), SimpleAnn("4324")})
  @java.lang.Override()
  public void foo(@Ann(x = 2, y = "324", z = Ann.class, e = {byte.class, Base.class}, depr = kotlin.DeprecationLevel.WARNING, t = {SimpleAnn("687"), SimpleAnn("78")}) @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  foo(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  Derived(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Base);//  .ctor(@org.jetbrains.annotations.NotNull() Base)
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface SimpleAnn /* SimpleAnn*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String value();//  value()
}
