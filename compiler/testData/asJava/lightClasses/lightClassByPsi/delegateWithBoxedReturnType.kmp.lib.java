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
  private final boolean booleanProperty;

  private final char charProperty;

  private final int intProperty;

  private final long longProperty;

  public  Delegating(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() Base);//  .ctor(@org.jetbrains.annotations.NotNull() Base)

  public boolean boolean();//  boolean()

  public boolean getBooleanProperty();//  getBooleanProperty()

  public char char();//  char()

  public char getCharProperty();//  getCharProperty()

  public int getIntProperty();//  getIntProperty()

  public int int();//  int()

  public long getLongProperty();//  getLongProperty()

  public long long();//  long()
}
