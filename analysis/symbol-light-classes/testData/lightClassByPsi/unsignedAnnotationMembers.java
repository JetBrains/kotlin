@Unsigned(ui = -2, ul = -2L, uis = {-2})
public final class Annotated /* Annotated*/ {
  public  Annotated();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Unsigned /* Unsigned*/ {
  public abstract byte ub() default -1;//  ub()

  public abstract int @org.jetbrains.annotations.NotNull() [] uis() default {1u, -1};//  uis()

  public abstract int i() default 1;//  i()

  public abstract int ui() default -1;//  ui()

  public abstract long ul() default -1L;//  ul()

  public abstract short us() default -1;//  us()
}
