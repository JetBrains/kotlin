@Unsigned(ui = -2, ul = -2L, uis = {-2})
public final class Annotated /* Annotated*/ {
  public  Annotated();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Unsigned /* Unsigned*/ {
  public abstract @org.jetbrains.annotations.NotNull() byte ub() default -1;//  ub()

  public abstract @org.jetbrains.annotations.NotNull() int ui() default -1;//  ui()

  public abstract @org.jetbrains.annotations.NotNull() long ul() default -1L;//  ul()

  public abstract @org.jetbrains.annotations.NotNull() short us() default -1;//  us()

  public abstract int @org.jetbrains.annotations.NotNull() [] uis() default {1u, -1};//  uis()

  public abstract int i() default 1;//  i()
}
