@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Annotation /* Annotation*/ {
  public abstract java.lang.String[] strings();//  strings()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnnotationArray /* AnnotationArray*/ {
  public abstract Annotation[] value();//  value()
}

@AnnotationArray({@Annotation(strings = {"[sar]1", "[sar]2"})})
public final class CA /* CA*/ {
  public  CA();//  .ctor()
}
