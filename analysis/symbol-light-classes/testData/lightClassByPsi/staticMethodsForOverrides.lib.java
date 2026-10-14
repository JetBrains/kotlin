public abstract interface Base /* Base*/ {
  public abstract int getProperty();//  getProperty()

  public abstract void function();//  function()

  public abstract void setProperty(int);//  setProperty(int)
}

public final class ClassWithCompanion /* ClassWithCompanion*/ {
  @org.jetbrains.annotations.NotNull()
  public static final ClassWithCompanion.Companion Companion;

  @kotlin.jvm.JvmStatic()
  public static void function();//  function()

  public  ClassWithCompanion();//  .ctor()

  public static int getProperty();//  getProperty()

  public static void setProperty(int);//  setProperty(int)

  class Companion ...
}

public static final class Companion /* ClassWithCompanion.Companion*/ implements Base {
  @kotlin.jvm.JvmStatic()
  public void function();//  function()

  private  Companion();//  .ctor()

  public int getProperty();//  getProperty()

  public void setProperty(int);//  setProperty(int)
}

public abstract interface InterfaceWithCompanion /* InterfaceWithCompanion*/ {
  @org.jetbrains.annotations.NotNull()
  public static final InterfaceWithCompanion.Companion Companion;

  @kotlin.jvm.JvmStatic()
  public static void function();//  function()

  public static int getProperty();//  getProperty()

  public static void setProperty(int);//  setProperty(int)

  class Companion ...
}

public static final class Companion /* InterfaceWithCompanion.Companion*/ implements Base {
  @kotlin.jvm.JvmStatic()
  public void function();//  function()

  private  Companion();//  .ctor()

  public int getProperty();//  getProperty()

  public void setProperty(int);//  setProperty(int)
}

public abstract interface InterfaceWithImplementations /* InterfaceWithImplementations*/ extends Base {
  public default int getProperty();//  getProperty()

  public default void function();//  function()

  public default void setProperty(int);//  setProperty(int)

  public static final class DefaultImpls /* InterfaceWithImplementations.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static int getProperty(@org.jetbrains.annotations.NotNull() InterfaceWithImplementations);//  getProperty(InterfaceWithImplementations)

    @java.lang.Deprecated()
    public static void function(@org.jetbrains.annotations.NotNull() InterfaceWithImplementations);//  function(InterfaceWithImplementations)

    @java.lang.Deprecated()
    public static void setProperty(@org.jetbrains.annotations.NotNull() InterfaceWithImplementations, int);//  setProperty(InterfaceWithImplementations, int)
  }
}
