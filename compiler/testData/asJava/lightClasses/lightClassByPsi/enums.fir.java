public final class C /* C*/ {
  @org.jetbrains.annotations.Nullable()
  private final Direction enumConst = Direction.EAST /* initializer type: Direction */;

  @org.jetbrains.annotations.Nullable()
  public final Direction getEnumConst();//  getEnumConst()

  public  C();//  .ctor()
}

public enum Color /* Color*/ {
  RED,
  GREEN,
  BLUE;

  private final int rgb = 5 /* initializer type: int */;

  @org.jetbrains.annotations.NotNull()
  public static Color valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static Color[] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<Color> getEntries();//  getEntries()

  private  Color(int);//  .ctor(int)

  private  Color(java.lang.String);//  .ctor(java.lang.String)

  public final int getRgb();//  getRgb()
}

public enum Direction /* Direction*/ {
  @Some() NORTH,
  SOUTH,
  WEST,
  EAST;

  @org.jetbrains.annotations.NotNull()
  public static Direction valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static Direction[] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<Direction> getEntries();//  getEntries()

  private  Direction();//  .ctor()
}

public abstract enum IntArithmetics /* IntArithmetics*/ implements java.util.function.BinaryOperator<java.lang.Integer>, java.util.function.IntBinaryOperator {
  PLUS {
   PLUS();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
  },
  TIMES {
   TIMES();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
  };

  @java.lang.Override()
  public int applyAsInt(int, int);//  applyAsInt(int, int)

  @org.jetbrains.annotations.NotNull()
  public static IntArithmetics valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static IntArithmetics[] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<IntArithmetics> getEntries();//  getEntries()

  private  IntArithmetics();//  .ctor()
}

static final class PLUS /* IntArithmetics.PLUS*/ extends IntArithmetics {
   PLUS();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
}

public abstract enum ProtocolState /* ProtocolState*/ {
  WAITING {
   WAITING();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
  },
  TALKING {
   TALKING();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
  };

  @org.jetbrains.annotations.NotNull()
  public abstract ProtocolState signal();//  signal()

  @org.jetbrains.annotations.NotNull()
  public static ProtocolState valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static ProtocolState[] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<ProtocolState> getEntries();//  getEntries()

  private  ProtocolState();//  .ctor()
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Some /* Some*/ {
}

static final class TALKING /* ProtocolState.TALKING*/ extends ProtocolState {
   TALKING();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
}

static final class TIMES /* IntArithmetics.TIMES*/ extends IntArithmetics {
   TIMES();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
}

static final class WAITING /* ProtocolState.WAITING*/ extends ProtocolState {
   WAITING();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
}
