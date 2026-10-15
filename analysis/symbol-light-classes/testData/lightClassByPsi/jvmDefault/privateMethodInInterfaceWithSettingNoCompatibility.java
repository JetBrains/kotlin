public abstract interface Foo /* Foo*/ {
  private default int getPrivateProperty();//  getPrivateProperty()

  private default void foo();//  foo()

  private default void setPrivateProperty(int);//  setPrivateProperty(int)

  public default void bar();//  bar()
}
