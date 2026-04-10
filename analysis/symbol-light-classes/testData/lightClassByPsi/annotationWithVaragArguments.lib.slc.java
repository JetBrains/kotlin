@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* A*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract int @org.jetbrains.annotations.NotNull() [] x();//  x()
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
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String x();//  x()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String z();//  z()

  @org.jetbrains.annotations.NotNull()
  public abstract int @org.jetbrains.annotations.NotNull() [] y();//  y()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() A a();//  a()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() B b();//  b()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface D /* D*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] x();//  x()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface E /* E*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() D d();//  d()
}
