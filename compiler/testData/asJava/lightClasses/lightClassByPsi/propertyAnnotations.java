@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Anno /* Anno*/ {
  public abstract java.lang.String p() default "";//  p()
}

public final class C /* C*/ {
  @org.jetbrains.annotations.NotNull()
  public static final C.Companion Companion;

  private static final int x;

  private static final int y;

  public  C();//  .ctor()

  public static final int getY();//  getY()

  class Companion ...
}

public static final class Companion /* C.Companion*/ {
  private  Companion();//  .ctor()

  public final int getX();//  getX()

  public final int getY();//  getY()
}

public class O /* O*/ {
  private final int protectedProperty;

  private final int getPrivateProperty();//  getPrivateProperty()

  protected final int getProtectedProperty();//  getProtectedProperty()

  public  O();//  .ctor()
}

public final class PropertyAnnotationsKt /* PropertyAnnotationsKt*/ {
  @kotlin.jvm.Transient()
  @kotlin.jvm.Volatile()
  private static transient volatile int jvmFlags;

  @org.jetbrains.annotations.NotNull()
  private static final java.lang.String nonNullable;

  @org.jetbrains.annotations.Nullable()
  private static final java.lang.String nullable;

  private static final int deprecated;

  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String getNonNullable();//  getNonNullable()

  @org.jetbrains.annotations.Nullable()
  public static final java.lang.String getNullable();//  getNullable()

  public static final <T> int getExtensionProperty(@Anno(p = "receiver") @org.jetbrains.annotations.NotNull() java.util.List<? extends T>);// <T>  getExtensionProperty(java.util.List<? extends T>)

  public static final int getDeprecated();//  getDeprecated()

  public static final int getJvmFlags();//  getJvmFlags()

  public static final void setJvmFlags(int);//  setJvmFlags(int)
}
