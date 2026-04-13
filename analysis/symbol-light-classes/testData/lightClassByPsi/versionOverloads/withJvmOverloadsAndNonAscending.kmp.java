@kotlin.OptIn(markerClass = {kotlin.ExperimentalVersionOverloading.class})
@kotlin.Suppress(names = {"CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION"})
public final class WithJvmOverloadsAndNonAscendingKt /* WithJvmOverloadsAndNonAscendingKt*/ {
  @<error>()
  public static final void emptyBase(@kotlin.IntroducedAt(version = "1") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int);//  emptyBase(@org.jetbrains.annotations.NotNull() java.lang.String, int)

  @<error>()
  public static final void nonAscending(int, @kotlin.IntroducedAt(version = "3") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "2") boolean);//  nonAscending(int, @org.jetbrains.annotations.NotNull() java.lang.String, boolean)

  @<error>()
  public static final void nonAscendingSameType(int, @kotlin.IntroducedAt(version = "3") int, @kotlin.IntroducedAt(version = "4") int);//  nonAscendingSameType(int, int, int)

  @<error>()
  public static final void random(int, @kotlin.IntroducedAt(version = "3") boolean, @kotlin.IntroducedAt(version = "2") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "4") long);//  random(int, boolean, @org.jetbrains.annotations.NotNull() java.lang.String, long)

  @<error>()
  public static final void randomAlmostSameType(@kotlin.IntroducedAt(version = "1.0-beta.1") int, int, @kotlin.IntroducedAt(version = "1.0") @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, @kotlin.IntroducedAt(version = "1.0-alpha.2") int);//  randomAlmostSameType(int, int, @org.jetbrains.annotations.NotNull() java.lang.String, int)

  @<error>()
  public static final void randomSameType(int, @kotlin.IntroducedAt(version = "3") int, @kotlin.IntroducedAt(version = "2") int, @kotlin.IntroducedAt(version = "4") int);//  randomSameType(int, int, int, int)
}
