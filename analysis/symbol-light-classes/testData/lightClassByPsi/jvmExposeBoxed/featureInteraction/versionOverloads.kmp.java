public final class Exposed /* Exposed*/ {
  @<error>()
  @<error>()
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  @kotlin.Suppress(names = {"CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION"})
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String foo(float);//  foo(float)

  @<error>()
  @<error>()
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to version 2.", level = kotlin.DeprecationLevel.ERROR)
  @kotlin.Suppress(names = {"CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION"})
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String foo(float, @kotlin.IntroducedAt(version = "2") boolean);//  foo(float, boolean)

  public  Exposed();//  .ctor()
}

public final class ExposedAndRenamed /* ExposedAndRenamed*/ {
  @<error>()
  @<error>()
  @<error>()
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to the initial version.", level = kotlin.DeprecationLevel.ERROR)
  @kotlin.Suppress(names = {"CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION"})
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String foo(float);//  foo(float)

  @<error>()
  @<error>()
  @<error>()
  @kotlin.Deprecated(message = "This method is kept for binary compatibility purposes, please use the main overload. This overload corresponds to version 2.", level = kotlin.DeprecationLevel.ERROR)
  @kotlin.Suppress(names = {"CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION"})
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String foo(float, @kotlin.IntroducedAt(version = "2") boolean);//  foo(float, boolean)

  public  ExposedAndRenamed();//  .ctor()
}
