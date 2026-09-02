public final class Exposed /* Exposed*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() java.lang.String renamedProperty = "" /* initializer type: java.lang.String */;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @<error>()
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String withBothNames(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withBothNames(@org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getRenamedProperty();//  getRenamedProperty()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String withDefaultName(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withDefaultName(@org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String withExposedName(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  withExposedName(@org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public  Exposed(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @<error>()
  public final void setRenamedProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setRenamedProperty(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class ExposedWithoutValueClassKt /* ExposedWithoutValueClassKt*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String topLevel(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  topLevel(@org.jetbrains.annotations.NotNull() java.lang.String)
}
