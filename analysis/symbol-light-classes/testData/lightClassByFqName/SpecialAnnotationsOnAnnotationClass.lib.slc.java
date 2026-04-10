@java.lang.annotation.Documented()
@java.lang.annotation.Repeatable(value = Anno.Container.class)
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.MustBeDocumented()
@kotlin.annotation.Repeatable()
@kotlin.annotation.Retention()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface Anno /* Anno*/ {
  public abstract int i();//  i()

  @java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
  @java.lang.annotation.Target()
  @kotlin.annotation.Retention()
  @kotlin.annotation.Target(allowedTargets = {})
  @kotlin.jvm.internal.RepeatableContainer()
  public static abstract @interface Container /* Anno.Container*/ {
    public abstract Anno[] value();//  value()
  }
}
