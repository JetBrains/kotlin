@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface AnotherUniversalAnnotation /* AnotherUniversalAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.FIELD})
public abstract @interface FieldAnnotation /* FieldAnnotation*/ {
}

public final class MyClass /* MyClass*/ {
  @AnotherUniversalAnnotation()
  private final int x5;

  @FieldAnnotation()
  private final int x1;

  @UniversalAnnotation()
  private final int x6;

  private final int x2;

  private final int x3;

  private final int x4;

  private final int x7;

  @AnotherUniversalAnnotation()
  @java.lang.Deprecated()
  public static void getX7$annotations();//  getX7$annotations()

  @PropertyAnnotation()
  @java.lang.Deprecated()
  public static void getX1$annotations();//  getX1$annotations()

  @PropertyOrFieldAnnotation()
  @java.lang.Deprecated()
  public static void getX2$annotations();//  getX2$annotations()

  @UniversalAnnotation()
  @java.lang.Deprecated()
  public static void getX5$annotations();//  getX5$annotations()

  public  MyClass(@AnotherUniversalAnnotation() @ParameterAnnotation() @UniversalAnnotation() int, int, @PropertyOrParameterAnnotation() int, @ParameterOrFieldAnnotation() int, int, @AnotherUniversalAnnotation() int, @UniversalAnnotation() int);//  .ctor(int, int, int, int, int, int, int)

  public final int getX1();//  getX1()

  public final int getX2();//  getX2()

  public final int getX3();//  getX3()

  public final int getX4();//  getX4()

  public final int getX5();//  getX5()

  public final int getX6();//  getX6()

  public final int getX7();//  getX7()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.VALUE_PARAMETER})
public abstract @interface ParameterAnnotation /* ParameterAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.FIELD, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER})
public abstract @interface ParameterOrFieldAnnotation /* ParameterOrFieldAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY})
public abstract @interface PropertyAnnotation /* PropertyAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY, kotlin.annotation.AnnotationTarget.FIELD})
public abstract @interface PropertyOrFieldAnnotation /* PropertyOrFieldAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER})
public abstract @interface PropertyOrParameterAnnotation /* PropertyOrParameterAnnotation*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY, kotlin.annotation.AnnotationTarget.FIELD, kotlin.annotation.AnnotationTarget.VALUE_PARAMETER})
public abstract @interface UniversalAnnotation /* UniversalAnnotation*/ {
}
