@kotlin.OptIn(markerClass = {kotlin.ExperimentalStdlibApi.class})
@kotlin.jvm.JvmInline()
public final class IntWrapper /* IntWrapper*/ {
  private final int i;

  @kotlin.jvm.JvmExposeBoxed()
  public  IntWrapper();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed()
  public  IntWrapper(int);//  .ctor(int)

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

  @kotlin.jvm.JvmExposeBoxed()
  public  RegularClassWithValueConstructorAndAnnotation();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed()
  public  RegularClassWithValueConstructorAndAnnotation(@org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(@org.jetbrains.annotations.NotNull() IntWrapper)

  private  RegularClassWithValueConstructorAndAnnotation(int);//  .ctor(int)
}
