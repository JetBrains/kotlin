public enum Event /* Event*/ {
  ON_CREATE,
  ON_START,
  ON_STOP,
  ON_DESTROY;

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() Event.Companion Companion;

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.Nullable()
  public static final @org.jetbrains.annotations.Nullable() Event upTo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() State);//  upTo(@org.jetbrains.annotations.NotNull() State)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Event @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() Event valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() Event> getEntries();//  getEntries()

  private  Event();//  .ctor()

  class Companion ...
}

public static final class Companion /* Event.Companion*/ {
  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.Nullable()
  public final @org.jetbrains.annotations.Nullable() Event upTo(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() State);//  upTo(@org.jetbrains.annotations.NotNull() State)

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
  public static final @org.jetbrains.annotations.NotNull() State.Companion Companion;

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() State @org.jetbrains.annotations.NotNull() [] values();//  values()

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() State valueOf(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;//  valueOf(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public static @org.jetbrains.annotations.NotNull() kotlin.enums.EnumEntries<@org.jetbrains.annotations.NotNull() State> getEntries();//  getEntries()

  private  State();//  .ctor()

  public final boolean isAtLeast(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() State);//  isAtLeast(@org.jetbrains.annotations.NotNull() State)

  public final boolean isFinished();//  isFinished()

  class Companion ...
}

public static final class Companion /* State.Companion*/ {
  private  Companion();//  .ctor()

  public final boolean done(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() State);//  done(@org.jetbrains.annotations.NotNull() State)
}
