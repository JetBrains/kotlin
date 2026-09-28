public final class Exposed /* Exposed*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  private java.lang.String renamedProperty;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String withDefaultName(@org.jetbrains.annotations.NotNull() java.lang.String);//  withDefaultName(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed()
  public  Exposed(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "exposedName")
  @kotlin.jvm.JvmName(name = "jvmName")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String jvmName(@org.jetbrains.annotations.NotNull() java.lang.String);//  jvmName(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "getRenamed")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getRenamed();//  getRenamed()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "renamed")
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String renamed(@org.jetbrains.annotations.NotNull() java.lang.String);//  renamed(java.lang.String)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "setRenamed")
  public final void setRenamed(@org.jetbrains.annotations.NotNull() java.lang.String);//  setRenamed(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getS();//  getS()
}

public final class ExposedWithoutValueClassKt /* ExposedWithoutValueClassKt*/ {
  @kotlin.jvm.JvmExposeBoxed(jvmName = "topLevelRenamed")
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String topLevelRenamed(@org.jetbrains.annotations.NotNull() java.lang.String);//  topLevelRenamed(java.lang.String)
}
