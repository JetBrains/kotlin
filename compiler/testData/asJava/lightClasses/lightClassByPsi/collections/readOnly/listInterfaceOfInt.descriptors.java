public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()
}

public abstract class CList2 /* test.CList2*/ implements test.IList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.Integer> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator(int);//  listIterator(int)

  public  CList2(@org.jetbrains.annotations.NotNull() test.IList);//  .ctor(test.IList)

  public boolean contains(int);//  contains(int)

  public boolean containsAll();//  containsAll()

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)
}

public class CList3 /* test.CList3*/ implements test.IList {
  @org.jetbrains.annotations.NotNull()
  public java.lang.Integer get(int);//  get(int)

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<java.lang.Integer> iterator();//  iterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.List<java.lang.Integer> subList(int, int);//  subList(int, int)

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator();//  listIterator()

  @org.jetbrains.annotations.NotNull()
  public java.util.ListIterator<java.lang.Integer> listIterator(int);//  listIterator(int)

  public  CList3();//  .ctor()

  public boolean contains(int);//  contains(int)

  public boolean containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection);//  containsAll(java.util.Collection)

  public boolean isEmpty();//  isEmpty()

  public int getSize();//  getSize()

  public int indexOf(int);//  indexOf(int)

  public int lastIndexOf(int);//  lastIndexOf(int)
}

public abstract interface IList /* test.IList*/ extends java.util.List<java.lang.Integer>, kotlin.collections.List<java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker {
}
