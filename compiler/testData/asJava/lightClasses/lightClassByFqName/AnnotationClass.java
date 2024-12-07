@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Anno /* Anno*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String value() default "a";//  value()

  public abstract double d() default 0.0;//  d()

  public abstract int @org.jetbrains.annotations.NotNull() [] ia();//  ia()

  public abstract int @org.jetbrains.annotations.NotNull() [] ia2() default {1, 2, 3};//  ia2()

  public abstract int i();//  i()

  public abstract int j() default 5;//  j()
}
