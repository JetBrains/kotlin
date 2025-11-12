public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()
}

public abstract class CList2 /* test.CList2*/ implements test.IList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator(int);//  listIterator(int)

  public  CList2(@org.jetbrains.annotations.NotNull() test.IList);//  .ctor(test.IList)

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(java.lang.String)
}

public class CList3 /* test.CList3*/ implements test.IList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.String> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.String> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.String> listIterator(int);//  listIterator(int)

  public  CList3();//  .ctor()

  public boolean contains(@org.jetbrains.annotations.NotNull() java.lang.String);//  contains(java.lang.String)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  indexOf(java.lang.String)

  public int lastIndexOf(@org.jetbrains.annotations.NotNull() java.lang.String);//  lastIndexOf(java.lang.String)
}

public abstract interface IList /* test.IList*/ extends java.util.List<java.lang.String>, kotlin.collections.List<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
}
