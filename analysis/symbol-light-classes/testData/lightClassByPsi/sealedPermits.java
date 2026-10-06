public class OpenSubclass /* test.OpenSubclass*/ extends test.SealedWithOpenSubclass {
  public  OpenSubclass();//  .ctor()
}

public abstract class SealedClass /* test.SealedClass*/ {
  private  SealedClass();//  .ctor()

  class Final ...

  class Obj ...
}

public static final class Final /* test.SealedClass.Final*/ extends test.SealedClass {
  public  Final();//  .ctor()
}

public static final class Obj /* test.SealedClass.Obj*/ extends test.SealedClass {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.SealedClass.Obj INSTANCE;

  private  Obj();//  .ctor()
}

public abstract interface SealedInterface /* test.SealedInterface*/ {
  class SealedInterfaceBase ...
}

public static final class SealedInterfaceBase /* test.SealedInterface.SealedInterfaceBase*/ implements test.SealedInterface {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.SealedInterface.SealedInterfaceBase INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.String toString();//  toString()

  private  SealedInterfaceBase();//  .ctor()

  public boolean equals(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(@org.jetbrains.annotations.Nullable() java.lang.Object)

  public int hashCode();//  hashCode()
}

public abstract class SealedWithOpenSubclass /* test.SealedWithOpenSubclass*/ {
  private  SealedWithOpenSubclass();//  .ctor()
}
