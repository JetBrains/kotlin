@<error>()
public final class StringWrapper /* StringWrapper*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String s;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() StringWrapper.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getS();//  getS()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* StringWrapper.Companion*/ {
  @<error>()
  public final void regularStaticFunction();//  regularStaticFunction()

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() StringWrapper getStaticVariable();//  getStaticVariable()

  @kotlin.jvm.JvmExposeBoxed()
  public final void setStaticVariable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() StringWrapper);//  setStaticVariable(@org.jetbrains.annotations.NotNull() StringWrapper)

  private  Companion();//  .ctor()

  public final int getRegularStaticVariable();//  getRegularStaticVariable()

  public final void setRegularStaticVariable(int);//  setRegularStaticVariable(int)
}
