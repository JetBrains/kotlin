public enum Event /* Event*/ {
  ON_CREATE,
  ON_START,
  ON_STOP,
  ON_DESTROY;

  @org.jetbrains.annotations.NotNull()
  public static final Event.Companion Companion;

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.Nullable()
  public static final Event upTo(@org.jetbrains.annotations.NotNull() State);//  upTo(State)

  @org.jetbrains.annotations.NotNull()
  public static Event valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static Event[] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<Event> getEntries();//  getEntries()

  private  Event();//  .ctor()

  class Companion ...
}

public static final class Companion /* Event.Companion*/ {
  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.Nullable()
  public final Event upTo(@org.jetbrains.annotations.NotNull() State);//  upTo(State)

  private  Companion();//  .ctor()
}

public enum State /* State*/ {
  ENQUEUED,
  RUNNING,
  SUCCEEDED,
  FAILED,
  BLOCKED,
  CANCELLED;

  @org.jetbrains.annotations.NotNull()
  public static final State.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public static State valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static State[] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static kotlin.enums.EnumEntries<State> getEntries();//  getEntries()

  private  State();//  .ctor()

  public final boolean isAtLeast(@org.jetbrains.annotations.NotNull() State);//  isAtLeast(State)

  public final boolean isFinished();//  isFinished()

  class Companion ...
}

public static final class Companion /* State.Companion*/ {
  private  Companion();//  .ctor()

  public final boolean done(@org.jetbrains.annotations.NotNull() State);//  done(State)
}
