public class OpenSubclass /* test.OpenSubclass*/ extends test.SealedWithOpenSubclass {
  public  OpenSubclass();//  .ctor()
}

public abstract sealed class SealedClass /* test.SealedClass*/ {
  private  SealedClass();//  .ctor()

  class Final ...

  class Obj ...
}

public static final class Final /* test.SealedClass.Final*/ extends test.SealedClass {
  public  Final();//  .ctor()
}

public static final class Obj /* test.SealedClass.Obj*/ extends test.SealedClass {
  @org.jetbrains.annotations.NotNull()
  public static final test.SealedClass.Obj INSTANCE;

  private  Obj();//  .ctor()
}

public abstract sealed interface SealedInterface /* test.SealedInterface*/ {
  class SealedInterfaceBase ...
}

public static final class SealedInterfaceBase /* test.SealedInterface.SealedInterfaceBase*/ implements test.SealedInterface {
  @org.jetbrains.annotations.NotNull()
  public static final test.SealedInterface.SealedInterfaceBase INSTANCE;

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  private  SealedInterfaceBase();//  .ctor()

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public int hashCode();//  hashCode()
}

public abstract sealed class SealedWithOpenSubclass /* test.SealedWithOpenSubclass*/ {
  private  SealedWithOpenSubclass();//  .ctor()
}
