public final class FullValueClassesKt /* full.FullValueClassesKt*/ {
  @org.jetbrains.annotations.NotNull()
  public static final full.MultiField topLevel(@org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() full.MultiField);//  topLevel(full.SingleField, full.MultiField)
}

public final class Marker /* full.Marker*/ {
  @org.jetbrains.annotations.NotNull()
  public static final full.Marker INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getLabel();//  getLabel()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String member();//  member()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  Marker();//  .ctor()

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()
}

public final class MultiField /* full.MultiField*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String y;

  @org.jetbrains.annotations.NotNull()
  public static final full.MultiField.Companion Companion;

  private final int x;

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public static final full.MultiField create(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  create(int, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public final full.MultiField replace(@org.jetbrains.annotations.NotNull() full.MultiField);//  replace(full.MultiField)

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getDescription();//  getDescription()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getY();//  getY()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  MultiField(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(int, java.lang.String)

  public  MultiField(long, @org.jetbrains.annotations.NotNull() java.lang.CharSequence);//  .ctor(long, java.lang.CharSequence)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getX();//  getX()

  public int hashCode();//  hashCode()

  class Companion ...
}

public static final class Companion /* full.MultiField.Companion*/ {
  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public final full.MultiField create(int, @org.jetbrains.annotations.NotNull() java.lang.String);//  create(int, java.lang.String)

  private  Companion();//  .ctor()
}

public final class SingleField /* full.SingleField*/ {
  @org.jetbrains.annotations.NotNull()
  private final java.lang.String value;

  @org.jetbrains.annotations.NotNull()
  public final full.SingleField duplicate();//  duplicate()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String getValue();//  getValue()

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  SingleField(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int getSize();//  getSize()

  public int hashCode();//  hashCode()
}

public final class Usage /* full.Usage*/ {
  @org.jetbrains.annotations.NotNull()
  private final full.MultiField multi;

  @org.jetbrains.annotations.NotNull()
  private final full.SingleField single;

  @org.jetbrains.annotations.NotNull()
  private full.MultiField mutable;

  @org.jetbrains.annotations.NotNull()
  public final /* vararg */ full.MultiField consumeAll(@org.jetbrains.annotations.NotNull() full.MultiField...);//  consumeAll(full.MultiField[])

  @org.jetbrains.annotations.NotNull()
  public final full.MultiField consume(@org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() full.MultiField);//  consume(full.SingleField, full.MultiField)

  @org.jetbrains.annotations.NotNull()
  public final full.MultiField getMulti();//  getMulti()

  @org.jetbrains.annotations.NotNull()
  public final full.MultiField getMutable();//  getMutable()

  @org.jetbrains.annotations.NotNull()
  public final full.SingleField getSingle();//  getSingle()

  public  Usage(@org.jetbrains.annotations.NotNull() full.SingleField, @org.jetbrains.annotations.NotNull() full.MultiField);//  .ctor(full.SingleField, full.MultiField)

  public final void setMutable(@org.jetbrains.annotations.NotNull() full.MultiField);//  setMutable(full.MultiField)
}
