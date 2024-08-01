public final class SmartSet /* SmartSet*/<T>  extends kotlin.collections.AbstractSet<T> implements java.util.Set<T>, kotlin.collections.MutableSet<T>, kotlin.jvm.internal.markers.KMutableSet {
  @org.jetbrains.annotations.NotNull()
  public static final SmartSet.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private java.lang.Object data = null /* initializer type: null */;

  private int size = 0 /* initializer type: int */;

  private static final int ARRAY_THRESHOLD = 5 /* initializer type: int */;

  @java.lang.Override()
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<T> iterator();//  iterator()

  @java.lang.Override()
  public boolean add(T);//  add(T)

  @java.lang.Override()
  public int getSize();//  getSize()

  @java.lang.Override()
  public void clear();//  clear()

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public static final <T> SmartSet<T> create();// <T>  create()

  @kotlin.jvm.JvmStatic()
  @org.jetbrains.annotations.NotNull()
  public static final <T> SmartSet<T> create(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends T>);// <T>  create(java.util.Collection<? extends T>)

  private  SmartSet();//  .ctor()

  public void setSize(int);//  setSize(int)

  public static final class Companion /* SmartSet.Companion*/ {
    @kotlin.jvm.JvmStatic()
    @org.jetbrains.annotations.NotNull()
    public final <T> SmartSet<T> create();// <T>  create()

    @kotlin.jvm.JvmStatic()
    @org.jetbrains.annotations.NotNull()
    public final <T> SmartSet<T> create(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends T>);// <T>  create(java.util.Collection<? extends T>)

    private  Companion();//  .ctor()
  }
}
