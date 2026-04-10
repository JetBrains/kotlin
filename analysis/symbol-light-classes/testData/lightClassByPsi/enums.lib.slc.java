public final class C /* C*/ {
  @org.jetbrains.annotations.Nullable()
  private final @org.jetbrains.annotations.Nullable() Direction enumConst;

  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() Direction getEnumConst();//  getEnumConst()

  public  C();//  .ctor()
}

public enum Color /* Color*/ {
  RED,
  GREEN,
  BLUE;

  private final int rgb;

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Color @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Color valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  Color(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  Color(int);//  .ctor(int)

  public final int getRgb();//  getRgb()
}

public enum Direction /* Direction*/ {
  @Some() NORTH,
  SOUTH,
  WEST,
  EAST;

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Direction @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Direction valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  Direction();//  .ctor()
}

public abstract enum IntArithmetics /* IntArithmetics*/ implements java.util.function.BinaryOperator<@org.jetbrains.annotations.NotNull() java.lang.Integer>, java.util.function.IntBinaryOperator {
  PLUS,
  TIMES;

  @java.lang.Override()
  public int applyAsInt(int, int);//  applyAsInt(int, int)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() IntArithmetics @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() IntArithmetics valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  IntArithmetics();//  .ctor()
}

public abstract enum ProtocolState /* ProtocolState*/ {
  WAITING,
  TALKING;

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() ProtocolState signal();//  signal()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() ProtocolState @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() ProtocolState valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  private  ProtocolState();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Some /* Some*/ {
}
