public abstract class AnnotatedValueMap /* test.AnnotatedValueMap*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, @test.TypeAnn() @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
  public  AnnotatedValueMap();//  .ctor()
}

public abstract class NotNullGetMap /* test.NotNullGetMap*/ implements java.util.Map<@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer get(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  get(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  NotNullGetMap();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.TYPE_USE})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.TYPE})
public abstract @interface TypeAnn /* test.TypeAnn*/ {
}
