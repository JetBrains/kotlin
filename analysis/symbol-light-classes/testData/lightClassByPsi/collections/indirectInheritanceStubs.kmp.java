public abstract class CCollection /* test.CCollection*/<Elem>  implements test.ICollection<Elem> {
  public  CCollection();//  .ctor()
}

public abstract class CCollection2 /* test.CCollection2*/<Elem>  extends test.CCollection<Elem> {
  public  CCollection2();//  .ctor()
}

public abstract class CCollection3 /* test.CCollection3*/ extends test.Foo implements java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  public  CCollection3();//  .ctor()
}

public abstract class CCollection4 /* test.CCollection4*/ extends test.Foo implements test.ICollection<@org.jetbrains.annotations.NotNull() java.lang.String> {
  public  CCollection4();//  .ctor()
}

public abstract class CCollection5 /* test.CCollection5*/ extends test.CCollection4 {
  public  CCollection5();//  .ctor()
}

public final class CCollection6 /* test.CCollection6*/ implements java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() test.CCollection6 INSTANCE;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  @java.lang.Override()
  public boolean contains(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  contains(@org.jetbrains.annotations.NotNull() java.lang.String)

  @java.lang.Override()
  public boolean containsAll(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>);//  containsAll(@org.jetbrains.annotations.NotNull() java.util.Collection<@org.jetbrains.annotations.NotNull() java.lang.String>)

  @java.lang.Override()
  public boolean isEmpty();//  isEmpty()

  @java.lang.Override()
  public int getSize();//  getSize()

  private  CCollection6();//  .ctor()
}

public abstract class CIterable /* test.CIterable*/ implements test.IIterable {
  public  CIterable();//  .ctor()
}

public abstract class CIterator /* test.CIterator*/ implements test.IIterator {
  public  CIterator();//  .ctor()
}

public abstract class CList /* test.CList*/ implements test.IList {
  public  CList();//  .ctor()
}

public abstract class CListIterator /* test.CListIterator*/ implements test.IListIterator {
  public  CListIterator();//  .ctor()
}

public abstract class Foo /* test.Foo*/ {
  public  Foo();//  .ctor()
}

public abstract interface ICollection /* test.ICollection*/<Elem>  extends java.util.Collection<Elem>, kotlin.jvm.internal.markers.KMappedMarker {
}

public abstract interface IIterable /* test.IIterable*/ extends java.lang.Iterable<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public default @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator();//  iterator()

  public static final class DefaultImpls /* test.IIterable.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String> iterator(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterable);//  iterator(@org.jetbrains.annotations.NotNull() test.IIterable)
  }
}

public abstract interface IIterator /* test.IIterator*/ extends java.util.Iterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  public default void remove();//  remove()

  public static final class DefaultImpls /* test.IIterator.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static void remove(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IIterator);//  remove(@org.jetbrains.annotations.NotNull() test.IIterator)
  }
}

public abstract interface IList /* test.IList*/ extends java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public default @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> subList(int, int);//  subList(int, int)

  public static final class DefaultImpls /* test.IList.DefaultImpls*/ {
    @java.lang.Deprecated()
    @org.jetbrains.annotations.NotNull()
    public static @org.jetbrains.annotations.NotNull() java.util.List<@org.jetbrains.annotations.NotNull() java.lang.String> subList(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IList, int, int);//  subList(@org.jetbrains.annotations.NotNull() test.IList, int, int)
  }
}

public abstract interface IListIterator /* test.IListIterator*/ extends java.util.ListIterator<@org.jetbrains.annotations.NotNull() java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @java.lang.Override()
  public default boolean hasNext();//  hasNext()

  public static final class DefaultImpls /* test.IListIterator.DefaultImpls*/ {
    @java.lang.Deprecated()
    public static boolean hasNext(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() test.IListIterator);//  hasNext(@org.jetbrains.annotations.NotNull() test.IListIterator)
  }
}
