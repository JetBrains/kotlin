@java.lang.annotation.Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
public abstract @interface Ann /* pkg.Ann*/ {
}

public final record MyRec /* pkg.MyRec*/(java.lang.String name, @pkg.Ann() int age, java.lang.String gender) {
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String component1();//  component1()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String component3();//  component3()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String gender();//  gender()

  @org.jetbrains.annotations.NotNull()
  public final java.lang.String name();//  name()

  @org.jetbrains.annotations.NotNull()
  public final pkg.MyRec copy(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @pkg.Ann() java.lang.String);//  copy(java.lang.String, int, java.lang.String)

  @org.jetbrains.annotations.NotNull()
  public java.lang.String toString();//  toString()

  public  MyRec(@org.jetbrains.annotations.NotNull() java.lang.String);//  .ctor(java.lang.String)

  public  MyRec(@org.jetbrains.annotations.NotNull() java.lang.String, int, @org.jetbrains.annotations.NotNull() @pkg.Ann() java.lang.String);//  .ctor(java.lang.String, int, java.lang.String)

  public boolean equals(@org.jetbrains.annotations.Nullable() java.lang.Object);//  equals(java.lang.Object)

  public final int age();//  age()

  public final int component2();//  component2()

  public int hashCode();//  hashCode()
}
