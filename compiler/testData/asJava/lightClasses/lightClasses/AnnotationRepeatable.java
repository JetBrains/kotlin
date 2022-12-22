@java.lang.annotation.Repeatable(value = simple.One.Container.class)
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@kotlin.annotation.Repeatable()
public abstract @interface One /* simple.One*/ {
  public abstract java.lang.String value();//  value()


@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@kotlin.jvm.internal.RepeatableContainer()
public static abstract @interface Container /* simple.One.Container*/ {
  public abstract simple.One[] value();//  value()

}}