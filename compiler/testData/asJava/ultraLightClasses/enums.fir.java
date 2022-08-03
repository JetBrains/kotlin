@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Some /* Some*/ {
}

public enum Direction /* Direction*/ {
  @Some() NORTH,
  SOUTH,
  WEST,
  EAST;

  @org.jetbrains.annotations.NotNull()
  public static Direction valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static Direction[] values();//  values()

  private  Direction();//  .ctor()

}

public enum Color /* Color*/ {
  RED,
  GREEN,
  BLUE;

  private final int rgb;

  @org.jetbrains.annotations.NotNull()
  public static Color valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static Color[] values();//  values()

  private  Color(int);//  .ctor(int)

  private  Color(java.lang.String);//  .ctor(java.lang.String)

  public final int getRgb();//  getRgb()

}

public enum ProtocolState /* ProtocolState*/ {
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
  public static ProtocolState valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static ProtocolState[] values();//  values()

  private  ProtocolState();//  .ctor()

}

public static final class WAITING /* ProtocolState.WAITING*/ extends ProtocolState {
   WAITING();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()

}

public static final class TALKING /* ProtocolState.TALKING*/ extends ProtocolState {
   TALKING();//  .ctor()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()

}

public enum IntArithmetics /* IntArithmetics*/ {
  PLUS {
     PLUS();//  .ctor()

    public int apply(int, int);//  apply(int, int)

  },
  TIMES {
     TIMES();//  .ctor()

    public int apply(int, int);//  apply(int, int)

  };

  @org.jetbrains.annotations.NotNull()
  public IntArithmetics applyAsInt(int, int);//  applyAsInt(int, int)

  @org.jetbrains.annotations.NotNull()
  public static IntArithmetics valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static IntArithmetics[] values();//  values()

  private  IntArithmetics();//  .ctor()

}

public static final class PLUS /* IntArithmetics.PLUS*/ extends IntArithmetics {
   PLUS();//  .ctor()

  public int apply(int, int);//  apply(int, int)

}

public static final class TIMES /* IntArithmetics.TIMES*/ extends IntArithmetics {
   TIMES();//  .ctor()

  public int apply(int, int);//  apply(int, int)

}

public final class C /* C*/ {
  @org.jetbrains.annotations.Nullable()
  private final Direction enumConst;

  @org.jetbrains.annotations.Nullable()
  public final Direction getEnumConst();//  getEnumConst()

  public  C();//  .ctor()

}
