public enum AnnotationsOnEnumEntry /* two.AnnotationsOnEnumEntry*/ {
  @two.PropertyImplicitly() @two.FieldImplicitly() @two.FieldExplicitly() EntryWithoutConstructor,
  @two.PropertyImplicitly() @two.FieldImplicitly() EntryWithConstructor,
  EntryWithConstructor2;

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<two.AnnotationsOnEnumEntry> getEntries();//  getEntries()

  @org.jetbrains.annotations.NotNull()
  public static two.AnnotationsOnEnumEntry valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static two.AnnotationsOnEnumEntry[] values();//  values()

  private  AnnotationsOnEnumEntry(int);//  .ctor(int)

  public final void foo();//  foo()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.FIELD})
public abstract @interface FieldExplicitly /* two.FieldExplicitly*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {java.lang.annotation.ElementType.FIELD})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.FIELD})
public abstract @interface FieldImplicitly /* two.FieldImplicitly*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY})
public abstract @interface PropertyExplicitly /* two.PropertyExplicitly*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value = {})
@kotlin.annotation.Target(allowedTargets = {kotlin.annotation.AnnotationTarget.PROPERTY})
public abstract @interface PropertyImplicitly /* two.PropertyImplicitly*/ {
}
