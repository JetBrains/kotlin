@<error>()
@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getI();//  getI()

  public int hashCode();//  hashCode()
}

public final class RegularClassWithValueConstructor /* RegularClassWithValueConstructor*/ {
  private final int property;

  private  RegularClassWithValueConstructor(int);//  .ctor(int)
}

@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
public final class RegularClassWithValueConstructorAndAnnotation /* RegularClassWithValueConstructorAndAnnotation*/ {
  private final int property;

  @<error>()
  private  RegularClassWithValueConstructorAndAnnotation(int);//  .ctor(int)
}
