@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* A*/ {
  public abstract int[] x();//  x()
}

public final class AnnotationWithVaragArgumentsKt /* AnnotationWithVaragArgumentsKt*/ {
  @A(x = {})
  @B(y = {})
  @C()
  @D()
  @E()
  public static final void foo();//  foo()

  @A(x = {})
  @B(y = {})
  @C()
  public static final void bar();//  bar()

  @A(x = {})
  @B(y = {})
  @C()
  public static final void baz();//  baz()
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
  public abstract java.lang.String[] x();//  x()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface E /* E*/ {
  public abstract D d();//  d()
}
