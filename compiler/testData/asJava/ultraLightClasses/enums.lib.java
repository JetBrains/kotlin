public abstract enum ProtocolState /* ProtocolState*/ {
  WAITING,
  TALKING;

  @org.jetbrains.annotations.NotNull()
  public abstract ProtocolState signal();//  signal()

  private  ProtocolState();//  .ctor()



  class TALKING ...

    class WAITING ...

  }
