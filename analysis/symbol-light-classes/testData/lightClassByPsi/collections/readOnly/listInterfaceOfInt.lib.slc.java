public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()
}

public abstract class CList2 /* test.CList2*/ implements test.IList {
  private final int size;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer get(int);//  get(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<java.lang.Integer> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<java.lang.Integer> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<java.lang.Integer> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  public  CList2(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IList);//  .ctor(@org.jetbrains.annotations.NotNull() test.IList)

  public boolean contains(int);//  contains(int)

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)
}

public class CList3 /* test.CList3*/ implements test.IList {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.lang.Integer get(int);//  get(int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.List<java.lang.Integer> subList(int, int);//  subList(int, int)

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<java.lang.Integer> listIterator();//  listIterator()

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.ListIterator<java.lang.Integer> listIterator(int);//  listIterator(int)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.Integer>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  public  CList3();//  .ctor()

  public boolean contains(int);//  contains(int)

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)
}

public abstract interface IList /* test.IList*/ extends java.util.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.collections.List<@org.jetbrains.annotations.NotNull() java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
