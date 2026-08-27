public final class Delegating /* Delegating*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String id;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Id getId();//  getId()

  @kotlin.jvm.JvmExposeBoxed()
  public  Delegating(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Id);//  .ctor(@org.jetbrains.annotations.NotNull() Id)

  private  Delegating(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  Delegating();//  .ctor()
}

@<error>()
public final class Id /* Id*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @kotlin.jvm.JvmExposeBoxed()
  public  Id(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public enum MyEnum /* MyEnum*/ {
  EXPLICIT,
  DEFAULT;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String id;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Id getId();//  getId()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() MyEnum @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() MyEnum valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() MyEnum> getEntries();//  getEntries()

  private  MyEnum(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)
}

public final class Regular /* Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String id;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() Id getId();//  getId()

  @kotlin.jvm.JvmExposeBoxed()
  public  Regular(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Id);//  .ctor(@org.jetbrains.annotations.NotNull() Id)

  private  Regular(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  Regular();//  .ctor()
}

public final class ResultHolder /* ResultHolder*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.Object result;

  @kotlin.jvm.JvmExposeBoxed()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String> getResult();//  getResult()

  @kotlin.jvm.JvmExposeBoxed()
  public  ResultHolder(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String>);//  .ctor(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends @org.jetbrains.annotations.NotNull() java.lang.String>)

  public  ResultHolder();//  .ctor()

  public  ResultHolder(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.Object);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.Object)
}
