public enum Direction /* Direction*/ {
  NORTH,
  SOUTH,
  WEST,
  EAST;

  private  Direction();//  .ctor()

}

public enum Color /* Color*/ {
  RED,
  GREEN,
  BLUE;

  private final int rgb;

  private  Color(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  private  Color(int);//  .ctor(int)

  public final int getRgb();//  getRgb()

}

public enum ProtocolState /* ProtocolState*/ {
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

  private  ProtocolState();//  .ctor()



  class TALKING ...
  
    class WAITING ...
  
  }

public static final class WAITING /* ProtocolState.WAITING*/ extends ProtocolState {
   WAITING();//  .ctor()

  @org.jetbrains.annotations.NotNull()
  public ProtocolState signal();//  signal()

}

public static final class TALKING /* ProtocolState.TALKING*/ extends ProtocolState {
   TALKING();//  .ctor()

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

  private  IntArithmetics();//  .ctor()



  class PLUS ...
  
    class TIMES ...
  
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