@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface A /* A*/ {
  public abstract int @org.jetbrains.annotations.NotNull() [] x();//  x()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface B /* B*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String x();//  x()

  public abstract @org.jetbrains.annotations.NotNull() java.lang.String z();//  z()

  public abstract int @org.jetbrains.annotations.NotNull() [] y();//  y()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface C /* C*/ {
  public abstract @org.jetbrains.annotations.NotNull() A a();//  a()

  public abstract @org.jetbrains.annotations.NotNull() B b();//  b()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface D /* D*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] x();//  x()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface E /* E*/ {
  public abstract @org.jetbrains.annotations.NotNull() D d();//  d()
}
