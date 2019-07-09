@java.lang.annotation.Retention(null=java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface SimpleAnn /* SimpleAnn*/ {
  @null()
  public abstract java.lang.String value();

}

@java.lang.annotation.Retention(null=java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* Ann*/ {
  @null()
  public abstract SimpleAnn[] t();

  @null()
  public abstract java.lang.Class<?> z();

  @null()
  public abstract java.lang.Class<?>[] e();

  @null()
  public abstract java.lang.String y();

  @null()
  public abstract kotlin.DeprecationLevel depr();

  public abstract int x();

}

public abstract interface Base /* Base*/ {
  @Ann(null=1, null="134", null=String::class, null=arrayOf(Int::class, Double::class), null=DeprecationLevel.WARNING, null=SimpleAnn("243"), null=SimpleAnn("4324"))
  public abstract void foo(@Ann(null=2, null="324", null=Ann::class, null=arrayOf(Byte::class, Base::class), null=DeprecationLevel.WARNING, null=SimpleAnn("687"), null=SimpleAnn("78")) @org.jetbrains.annotations.NotNull() java.lang.String);

}

public final class Derived /* Derived*/ implements Base {
  @Ann(x=1, y="134", z=java.lang.String.class, e={int.class, double.class}, depr=kotlin.DeprecationLevel.WARNING, t={@SimpleAnn(value="243"), @SimpleAnn(value="4324")})
  public void foo(@Ann(x=2, y="324", z=Ann.class, e={byte.class, Base.class}, depr=kotlin.DeprecationLevel.WARNING, t={@SimpleAnn(value="687"), @SimpleAnn(value="78")}) @org.jetbrains.annotations.NotNull() java.lang.String);

  @null()
  public  Derived(@org.jetbrains.annotations.NotNull() Base);

}
