public final class Delegating /* Delegating*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String id;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final Id getId();//  getId()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Delegating(@org.jetbrains.annotations.NotNull() Id);//  .ctor(Id)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getId-eEFUqEU();//  getId-eEFUqEU()

  private  Delegating(java.lang.String);//  .ctor(java.lang.String)

  public  Delegating();//  .ctor()
}

@kotlin.jvm.JvmInline()
public final class Id /* Id*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String value;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Id(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public static java.lang.String constructor-impl(@org.jetbrains.annotations.NotNull() java.lang.String);//  constructor-impl(java.lang.String)

  public boolean equals(java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()

  public java.lang.String toString();//  toString()

  public static boolean equals-impl(java.lang.String, java.lang.Object);//  equals-impl(java.lang.String, java.lang.Object)

  public static final boolean equals-impl0(java.lang.String, java.lang.String);//  equals-impl0(java.lang.String, java.lang.String)

  public static int hashCode-impl(java.lang.String);//  hashCode-impl(java.lang.String)

  public static java.lang.String toString-impl(java.lang.String);//  toString-impl(java.lang.String)
}

public enum MyEnum /* MyEnum*/ {
  EXPLICIT,
  DEFAULT;

  @org.jetbrains.annotations.NotNull()
  private final java.lang.String id;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final Id getId();//  getId()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getId-eEFUqEU();//  getId-eEFUqEU()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() MyEnum @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() MyEnum valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() MyEnum> getEntries();//  getEntries()

  private  MyEnum(java.lang.String);//  .ctor(java.lang.String)
}

public final class Regular /* Regular*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String id;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final Id getId();//  getId()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  Regular(@org.jetbrains.annotations.NotNull() Id);//  .ctor(Id)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getId-eEFUqEU();//  getId-eEFUqEU()

  private  Regular(java.lang.String);//  .ctor(java.lang.String)

  public  Regular();//  .ctor()
}

public final class ResultHolder /* ResultHolder*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.Object result;

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  @org.jetbrains.annotations.NotNull()
  public final kotlin.Result<? extends java.lang.String> getResult();//  getResult()

  @kotlin.jvm.JvmExposeBoxed(jvmName = "")
  public  ResultHolder(@org.jetbrains.annotations.NotNull() kotlin.Result<? extends java.lang.String>);//  .ctor(kotlin.Result<? extends java.lang.String>)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.Object getResult-d1pmJ48();//  getResult-d1pmJ48()

  public  ResultHolder();//  .ctor()

  public  ResultHolder(@org.jetbrains.annotations.NotNull() java.lang.Object);//  .ctor(java.lang.Object)
}
