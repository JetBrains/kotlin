public abstract class CCollection /* test.CCollection*/ implements test.ICollection {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/ implements test.ICollection {
  private final int size;

  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  public  CCollection2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ICollection);//  .ctor(@org.jetbrains.annotations.NotNull() test.ICollection)

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public class CCollection3 /* test.CCollection3*/ implements test.ICollection {
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  public  CCollection3();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()
}

public abstract interface ICollection /* test.ICollection*/ extends java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
