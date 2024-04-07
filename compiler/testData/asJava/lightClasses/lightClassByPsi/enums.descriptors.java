public final class C /* C*/ {
  @org.jetbrains.annotations.Nullable()
  private final Direction enumConst;

  @org.jetbrains.annotations.Nullable()
  public final Direction getEnumConst();//  getEnumConst()

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
  public static @org.jetbrains.annotations.NotNull() Color valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<Color> getEntries();//  getEntries()

  private  Color(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

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
  public static @org.jetbrains.annotations.NotNull() Direction valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<Direction> getEntries();//  getEntries()

  private  Direction();//  .ctor()
}

public abstract enum IntArithmetics /* IntArithmetics*/ implements java.util.function.BinaryOperator<java.lang.Integer>, java.util.function.IntBinaryOperator {
  PLUS {
   PLUS();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
  },
  TIMES {
   TIMES();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
  };

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() IntArithmetics @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() IntArithmetics valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<IntArithmetics> getEntries();//  getEntries()

  private  IntArithmetics();//  .ctor()

  public int applyAsInt(int, int);//  applyAsInt(int, int)

  class PLUS ...

  class TIMES ...
}

public static final class PLUS /* IntArithmetics.PLUS*/ extends IntArithmetics {
   PLUS();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
}

public abstract enum ProtocolState /* ProtocolState*/ {
  WAITING {
   WAITING();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
  },
  TALKING {
   TALKING();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
  };

  @org.jetbrains.annotations.NotNull()
  public abstract ProtocolState signal();//  signal()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() ProtocolState @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() ProtocolState valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<ProtocolState> getEntries();//  getEntries()

  private  ProtocolState();//  .ctor()

  class TALKING ...

  class WAITING ...
}

@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Some /* Some*/ {
}

public static final class TALKING /* ProtocolState.TALKING*/ extends ProtocolState {
   TALKING();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
}

public static final class TIMES /* IntArithmetics.TIMES*/ extends IntArithmetics {
   TIMES();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer apply(int, int);//  apply(int, int)
}

public static final class WAITING /* ProtocolState.WAITING*/ extends ProtocolState {
   WAITING();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()
}
