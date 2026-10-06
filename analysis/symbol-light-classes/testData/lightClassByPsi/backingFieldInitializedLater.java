public final class AnnotatedField /* test.AnnotatedField*/ {
  public  AnnotatedField();//  .ctor()

  public final int getFoo();//  getFoo()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.FIELD})
public abstract @interface FieldAnn /* test.FieldAnn*/ {
}

public final class InitBlock /* test.InitBlock*/ {
  public  InitBlock();//  .ctor()

  public final int getBar();//  getBar()

  public final int getFoo();//  getFoo()
}

public final class SecondaryConstructor /* test.SecondaryConstructor*/ {
  public  SecondaryConstructor(int);//  .ctor(int)

  public final int getFoo();//  getFoo()
}
