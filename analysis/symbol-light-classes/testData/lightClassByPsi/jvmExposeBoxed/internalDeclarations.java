@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class InternalNames /* InternalNames*/ {
  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() ValueInt jvmNamed(@org.jetbrains.annotations.NotNull() ValueInt);//  jvmNamed(@org.jetbrains.annotations.NotNull() ValueInt)

  @kotlin.jvm.JvmExposeBoxed(jvmName = "explicitBoxedName")
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() ValueInt explicitBoxedName(@org.jetbrains.annotations.NotNull() ValueInt);//  explicitBoxedName(@org.jetbrains.annotations.NotNull() ValueInt)

  @kotlin.jvm.JvmName(name = "ignoredJvmName")
  public final int ignoredJvmName(int);//  ignoredJvmName(int)

  @kotlin.jvm.JvmName(name = "jvmNamed")
  public final int jvmNamed(int);//  jvmNamed(int)

  public  InternalNames();//  .ctor()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmInline()
public final class ValueInt /* ValueInt*/ {
  private final int i;

  @kotlin.PublishedApi()
  @kotlin.jvm.JvmExposeBoxed()
  public final void publishedApiFun();//  publishedApiFun()

  @kotlin.jvm.JvmExposeBoxed()
  public  ValueInt(int);//  .ctor(int)

  @kotlin.jvm.JvmExposeBoxed()
  public final int getInternalProperty$light_idea_test_case();//  getInternalProperty$light_idea_test_case()

  @kotlin.jvm.JvmExposeBoxed()
  public final int getPublishedApiProperty$light_idea_test_case();//  getPublishedApiProperty$light_idea_test_case()

  @kotlin.jvm.JvmExposeBoxed()
  public final void internalFun$light_idea_test_case();//  internalFun$light_idea_test_case()

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}
