public abstract interface Test /* Test*/ {
  @org.jetbrains.annotations.NotNull()
  public abstract A foo();//  foo()

  @org.jetbrains.annotations.NotNull()
  public abstract A fooAliased();//  fooAliased()

  @org.jetbrains.annotations.NotNull()
  public abstract B<A, B<A, java.lang.String>> bar();//  bar()

  @org.jetbrains.annotations.NotNull()
  public abstract B<A, B<A, java.lang.String>> barAliased();//  barAliased()
}
