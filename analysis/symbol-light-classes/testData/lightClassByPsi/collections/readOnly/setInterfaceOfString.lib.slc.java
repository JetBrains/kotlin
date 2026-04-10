public abstract class CSet /* test.CSet*/ implements test.ISet {
  public  CSet();//  .ctor()
}

public abstract class CSet2 /* test.CSet2*/ implements test.ISet {
  private final int size;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  public  CSet2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.ISet);//  .ctor(@org.jetbrains.annotations.NotNull() test.ISet)

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getSize();//  getSize()
}

public class CSet3 /* test.CSet3*/ implements test.ISet {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  public  CSet3();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  public int getSize();//  getSize()
}

public abstract interface ISet /* test.ISet*/ extends java.util.Set<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.collections.Set<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
