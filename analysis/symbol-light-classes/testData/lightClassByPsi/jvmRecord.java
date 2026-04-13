@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* pkg.Ann*/ {
}

@kotlin.jvm.JvmRecord()
public final record MyRec /* pkg.MyRec*/(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String name, @pkg.Ann() int age, @org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String gender) {
  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String gender;

  @org.jetbrains.annotations.NotNull()
  private final @org.jetbrains.annotations.NotNull() java.lang.String name;

  @pkg.Ann()
  private final int age;

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String component1();//  component1()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String component3();//  component3()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String gender();//  gender()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() java.lang.String name();//  name()

  @org.jetbrains.annotations.NotNull()
  public final @org.jetbrains.annotations.NotNull() pkg.MyRec copy(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @pkg.Ann() @org.jetbrains.annotations.NotNull() java.lang.String);//  copy(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String)

  public  MyRec(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String)

  public  MyRec(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @pkg.Ann() @org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() java.lang.String)

  public final int age();//  age()

  public final int component2();//  component2()
}
