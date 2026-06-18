@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Annotation /* Annotation*/ {
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String @org.jetbrains.annotations.NotNull() [] strings();//  strings()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnnotationArray /* AnnotationArray*/ {
  public abstract @org.jetbrains.annotations.NotNull() Annotation @org.jetbrains.annotations.NotNull() [] value();//  value()
}

@AnnotationArray(value = {@Annotation(strings = {"[sar]1", "[sar]2"})})
public final class CA /* CA*/ {
  public  CA();//  .ctor()
}
