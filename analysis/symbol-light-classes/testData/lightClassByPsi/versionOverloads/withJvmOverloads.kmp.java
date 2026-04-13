@kotlin.OptIn(markerClass = {kotlin.ExperimentalVersionOverloading.class})
@kotlin.Suppress(names = {"CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION"})
public final class WithJvmOverloadsKt /* WithJvmOverloadsKt*/ {
  @<error>()
  public static final void ascending1(int, @kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  ascending1(int, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public static final void ascending2(int, @kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "2") float);//  ascending2(int, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String, float)

  @<error>()
  public static final void ascending3(int, @kotlin.IntroducedAt(version = "1.0-alpha.2") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "1.0-alpha.2") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "1.0-beta.1") float, @kotlin.IntroducedAt(version = "1.0") int);//  ascending3(int, @org.jetbrains.annotations.NotNull() java.lang.String, @org.jetbrains.annotations.NotNull() java.lang.String, float, int)

  @<error>()
  public static final void emptyBase(int, @kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  emptyBase(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public static final void sameType(int, @kotlin.IntroducedAt(version = "1") int, @kotlin.IntroducedAt(version = "2") int);//  sameType(int, int, int)
}
