public final class SmartSet /* SmartSet*/<T>  implements kotlin.collections.MutableSet<T> {
  @org.jetbrains.annotations.NotNull()
  public static final SmartSet.Companion Companion;

  @org.jetbrains.annotations.Nullable()
  private java.lang.Object data;

  private int size;

  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<T> iterator();//  iterator()

  private  SmartSet();//  .ctor()

  public boolean add(T);//  add(T)

  public int getSize();//  getSize()

  public void clear();//  clear()

  public void setSize(int);//  setSize(int)


public static final class Companion /* SmartSet.Companion*/ {
  private final int ARRAY_THRESHOLD;

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final <T> SmartSet<T> create();// <T>  create()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public final <T> SmartSet<T> create(@org.jetbrains.annotations.NotNull() java.util.Collection<? extends T>);// <T>  create(java.util.Collection<? extends T>)

  private  Companion();//  .ctor()

}}
