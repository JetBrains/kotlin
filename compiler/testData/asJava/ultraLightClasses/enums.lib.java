public abstract enum ProtocolState /* ProtocolState*/ {
  WAITING,
  TALKING;

  @org.jetbrains.annotations.NotNull()
  public abstract ProtocolState signal();//  signal()

  @org.jetbrains.annotations.NotNull()
  public static ProtocolState valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static ProtocolState[] values();//  values()

  private  ProtocolState();//  .ctor()



  class TALKING ...

    class WAITING ...

  }
