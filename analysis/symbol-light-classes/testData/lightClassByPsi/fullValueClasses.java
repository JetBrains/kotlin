public final class FullValueClassesKt /* full.FullValueClassesKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() full.MultiField topLevel(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.MultiField);//  topLevel(@org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() full.MultiField)
}

public final class Marker /* full.Marker*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() full.Marker INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getLabel();//  getLabel()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String member();//  member()

  private  Marker();//  .ctor()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class MultiField /* full.MultiField*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String y;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() full.MultiField.Companion Companion;

  private final int x;

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() full.MultiField create(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  create(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.MultiField replace(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.MultiField);//  replace(@org.jetbrains.annotations.NotNull() full.MultiField)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getDescription();//  getDescription()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getY();//  getY()

  public  MultiField(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  public  MultiField(long, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.CharSequence);//  .ctor(long, @org.jetbrains.annotations.NotNull() java.lang.CharSequence)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getX();//  getX()

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* full.MultiField.Companion*/ {
  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.MultiField create(int, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  create(int, @org.jetbrains.annotations.NotNull() java.lang.String)

  private  Companion();//  .ctor()
}

public final class SingleField /* full.SingleField*/ {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.SingleField duplicate();//  duplicate()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String getValue();//  getValue()

  public  SingleField(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public final int getSize();//  getSize()

  public int hashCode();//  hashCode()
}

public final class Usage /* full.Usage*/ {
  @org.jetbrains.annotations.NotNull()
  private @org.jetbrains.annotations.NotNull() full.MultiField mutable;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() full.MultiField multi;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() full.SingleField single;

  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ @org.jetbrains.annotations.NotNull() full.MultiField consumeAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.MultiField @org.jetbrains.annotations.NotNull() ...);//  consumeAll(@org.jetbrains.annotations.NotNull() full.MultiField @org.jetbrains.annotations.NotNull() [])

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.MultiField consume(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.MultiField);//  consume(@org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() full.MultiField)

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.MultiField getMulti();//  getMulti()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.MultiField getMutable();//  getMutable()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() full.SingleField getSingle();//  getSingle()

  public  Usage(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.MultiField);//  .ctor(@org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() full.MultiField)

  public final void setMutable(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() full.MultiField);//  setMutable(@org.jetbrains.annotations.NotNull() full.MultiField)
}
