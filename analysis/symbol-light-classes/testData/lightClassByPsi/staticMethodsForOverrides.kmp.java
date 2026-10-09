public abstract interface Base /* Base*/ {
  public abstract int getProperty();//  getProperty()

  public abstract void function();//  function()

  public abstract void setProperty(int);//  setProperty(int)
}

public final class ClassWithCompanion /* ClassWithCompanion*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() ClassWithCompanion.Companion Companion;

  public  ClassWithCompanion();//  .ctor()

  class Companion ...
}

public static final class Companion /* ClassWithCompanion.Companion*/ implements Base {
  @java.lang.Override()
  public int getProperty();//  getProperty()

  @java.lang.Override()
  public void function();//  function()

  @java.lang.Override()
  public void setProperty(int);//  setProperty(int)

  private  Companion();//  .ctor()
}

public abstract interface InterfaceWithCompanion /* InterfaceWithCompanion*/ {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() InterfaceWithCompanion.Companion Companion;

  class Companion ...
}

public static final class Companion /* InterfaceWithCompanion.Companion*/ implements Base {
  @java.lang.Override()
  public int getProperty();//  getProperty()

  @java.lang.Override()
  public void function();//  function()

  @java.lang.Override()
  public void setProperty(int);//  setProperty(int)

  private  Companion();//  .ctor()
}

public abstract interface InterfaceWithImplementations /* InterfaceWithImplementations*/ extends Base {
  @java.lang.Override()
  public default int getProperty();//  getProperty()

  @java.lang.Override()
  public default void function();//  function()

  @java.lang.Override()
  public default void setProperty(int);//  setProperty(int)

  public static final class DefaultImpls /* InterfaceWithImplementations.DefaultImpls*/ {
    public static int getProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() InterfaceWithImplementations);//  getProperty(@org.jetbrains.annotations.NotNull() InterfaceWithImplementations)

    public static void function(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() InterfaceWithImplementations);//  function(@org.jetbrains.annotations.NotNull() InterfaceWithImplementations)

    public static void setProperty(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() InterfaceWithImplementations, int);//  setProperty(@org.jetbrains.annotations.NotNull() InterfaceWithImplementations, int)
  }
}
