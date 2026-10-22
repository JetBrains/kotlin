@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface KtAnnotation /* KtAnnotation*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() KtAnnotation.Companion Companion;

  public static final int CONSTANT = 2 /* initializer type: int */ /* constant value 2 */;

  public abstract int i();//  i()

  class Companion ...
}

public static final class Companion /* KtAnnotation.Companion*/ {
  @org.jetbrains.annotations.NotNull()
  private static final @org.jetbrains.annotations.NotNull() java.lang.String stringProperty = "" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private static final @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?> kClassProperty;

  private static final int staticProperty = 1 /* initializer type: int */;

  public static @org.jetbrains.annotations.NotNull() java.lang.String lateinitVariable;

  public static final int CONSTANT = 2 /* initializer type: int */ /* constant value 2 */;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getLateinitVariable();//  getLateinitVariable()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getStaticVariable();//  getStaticVariable()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getStringProperty();//  getStringProperty()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?> getKClassProperty();//  getKClassProperty()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?> staticFun(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?>);//  staticFun(@org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() kotlin.reflect.KClass<?>)

  private  Companion();//  .ctor()

  public final int getStaticProperty();//  getStaticProperty()

  public final void companionFun();//  companionFun()

  public final void setLateinitVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setLateinitVariable(@org.jetbrains.annotations.NotNull() java.lang.String)

  public final void setStaticVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setStaticVariable(@org.jetbrains.annotations.NotNull() java.lang.String)
}
