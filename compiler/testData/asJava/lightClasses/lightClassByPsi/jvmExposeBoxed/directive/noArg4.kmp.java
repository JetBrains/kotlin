@<error>()
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

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper getProperty();//  getProperty()

  @kotlin.jvm.JvmExposeBoxed()
  public  RegularClassWithValueConstructor();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed()
  public  RegularClassWithValueConstructor(@org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(@org.jetbrains.annotations.NotNull() IntWrapper)

  private  RegularClassWithValueConstructor(int);//  .ctor(int)
}

public final class RegularClassWithValueConstructorAndAnnotation /* RegularClassWithValueConstructorAndAnnotation*/ {
  private final int property;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() IntWrapper getProperty();//  getProperty()

  @kotlin.jvm.JvmExposeBoxed()
  public  RegularClassWithValueConstructorAndAnnotation();//  .ctor()

  @kotlin.jvm.JvmExposeBoxed()
  public  RegularClassWithValueConstructorAndAnnotation(@org.jetbrains.annotations.NotNull() IntWrapper);//  .ctor(@org.jetbrains.annotations.NotNull() IntWrapper)

  private  RegularClassWithValueConstructorAndAnnotation(int);//  .ctor(int)
}
