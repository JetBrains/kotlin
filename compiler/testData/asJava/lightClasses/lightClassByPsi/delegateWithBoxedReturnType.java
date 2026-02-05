public abstract interface Base /* Base*/ {
  public abstract boolean boolean();//  boolean()

  public abstract boolean getBooleanProperty();//  getBooleanProperty()

  public abstract char char();//  char()

  public abstract char getCharProperty();//  getCharProperty()

  public abstract int getIntProperty();//  getIntProperty()

  public abstract int int();//  int()

  public abstract long getLongProperty();//  getLongProperty()

  public abstract long long();//  long()
}

public final class Delegating /* Delegating*/ implements Base {
  @java.lang.Override()
  public boolean boolean();//  boolean()

  @java.lang.Override()
  public boolean getBooleanProperty();//  getBooleanProperty()

  @java.lang.Override()
  public char char();//  char()

  @java.lang.Override()
  public char getCharProperty();//  getCharProperty()

  @java.lang.Override()
  public int getIntProperty();//  getIntProperty()

  @java.lang.Override()
  public int int();//  int()

  @java.lang.Override()
  public long getLongProperty();//  getLongProperty()

  @java.lang.Override()
  public long long();//  long()

  public  Delegating(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Base);//  .ctor(@org.jetbrains.annotations.NotNull() Base)
}
