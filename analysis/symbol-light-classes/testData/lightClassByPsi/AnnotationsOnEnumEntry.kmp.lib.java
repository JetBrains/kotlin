public enum AnnotationsOnEnumEntry /* two.AnnotationsOnEnumEntry*/ {
  @two.PropertyImplicitly() @two.FieldImplicitly() @two.FieldExplicitly() EntryWithoutConstructor,
  @two.PropertyImplicitly() @two.FieldImplicitly() EntryWithConstructor,
  EntryWithConstructor2;

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() two.AnnotationsOnEnumEntry @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() two.AnnotationsOnEnumEntry valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  AnnotationsOnEnumEntry(int);//  .ctor(int)

  public final void foo();//  foo()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface FieldExplicitly /* two.FieldExplicitly*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface FieldImplicitly /* two.FieldImplicitly*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface PropertyExplicitly /* two.PropertyExplicitly*/ {
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target()
@kotlin.annotation.Target(allowedTargets = {})
public abstract @interface PropertyImplicitly /* two.PropertyImplicitly*/ {
}
