@<error>()
@java.lang.annotation.Repeatable(value = Two.Container.class)
@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@kotlin.annotation.Repeatable()
public abstract @interface Two /* Two*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String name();//  name()

  @java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
  @kotlin.jvm.internal.RepeatableContainer()
  public static abstract @interface Container /* Two.Container*/ {
    public abstract Two[] value();//  value()
  }
}
