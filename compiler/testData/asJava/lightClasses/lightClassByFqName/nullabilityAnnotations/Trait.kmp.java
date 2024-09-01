public abstract interface Trait /* Trait*/ {
  @<error>()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String notNullWithN();//  notNullWithN()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String notNullWithNN();//  notNullWithNN()

  @<error>()
  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.String nullableWithN();//  nullableWithN()

  @<error>()
  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.String nullableWithNN();//  nullableWithNN()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getNotNullVal();//  getNotNullVal()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String getNotNullVar();//  getNotNullVar()

  @org.jetbrains.annotations.NotNull()
  public abstract @org.jetbrains.annotations.NotNull() java.lang.String notNull(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  notNull(@org.jetbrains.annotations.NotNull() java.lang.String)

  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.String getNullableVal();//  getNullableVal()

  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.String getNullableVar();//  getNullableVar()

  @org.jetbrains.annotations.Nullable()
  public abstract @org.jetbrains.annotations.Nullable() java.lang.String nullable(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String);//  nullable(@org.jetbrains.annotations.Nullable() java.lang.String)

  public abstract void setNotNullVar(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  setNotNullVar(@org.jetbrains.annotations.NotNull() java.lang.String)

  public abstract void setNullableVar(@org.jetbrains.annotations.Nullable() @org.jetbrains.annotations.Nullable() java.lang.String);//  setNullableVar(@org.jetbrains.annotations.Nullable() java.lang.String)
}
