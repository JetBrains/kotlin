@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* A*/ {
  public abstract int[] x();//  x()
}

public final class AnnotationWithVaragArgumentsKt /* AnnotationWithVaragArgumentsKt*/ {
  @A()
  @B(x = "x", z = "z")
  @C(a = @A(), b = @B(x = "x", z = "z"))
  @D()
  @E(d = @D())
  public static final void foo();//  foo()

  @A(x = {1, 2})
  @B(x = "x", y = {1, 2}, z = "z")
  @C(a = @A(x = {1, 2}), b = @B(x = "x", y = {1, 2}, z = "z"))
  public static final void baz();//  baz()

  @A(x = {1})
  @B(x = "x", y = {1}, z = "z")
  @C(a = @A(x = {1}), b = @B(x = "x", y = {1}, z = "z"))
  public static final void bar();//  bar()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface B /* B*/ {
  public abstract int[] y();//  y()

  public abstract java.lang.String x();//  x()

  public abstract java.lang.String z();//  z()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface C /* C*/ {
  public abstract A a();//  a()

  public abstract B b();//  b()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface D /* D*/ {
  public abstract java.lang.String[] x() default {"a", "b"};//  x()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface E /* E*/ {
  public abstract D d();//  d()
}
