public final class ValueClassInParametersWithJvmNameKt /* one.ValueClassInParametersWithJvmNameKt*/ {
  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String getter;

  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String nothing;

  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String setter;

  @org.jetbrains.annotations.NotNull()
  private static @org.jetbrains.annotations.NotNull() java.lang.String setterAndGetter;

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getGetter();//  getGetter()

  @<error>()
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getSetterAndGetter();//  getSetterAndGetter()

  @<error>()
  @org.jetbrains.annotations.Nullable()
  public static final @org.jetbrains.annotations.Nullable() java.lang.String functionWithValueClassInReturnWithJvmName();//  functionWithValueClassInReturnWithJvmName()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getNothing();//  getNothing()

  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String getSetter();//  getSetter()

  @org.jetbrains.annotations.Nullable()
  public static final @org.jetbrains.annotations.Nullable() java.lang.String functionWithValueClassInReturn();//  functionWithValueClassInReturn()
}
