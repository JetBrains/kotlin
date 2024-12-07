public final class MyException /* MyException*/ extends kotlin.Exception {
  public  MyException();//  .ctor()
}

public final class ThrowsAnnotationKt /* ThrowsAnnotationKt*/ {
  @kotlin.Throws(exceptionClasses = {java.io.IOException.class, MyException.class})
  @org.jetbrains.annotations.NotNull()
  public static final @org.jetbrains.annotations.NotNull() java.lang.String readFile(@org.jetbrains.annotations.NotNull() @org.jetbrains.annotations.NotNull() java.lang.String);//  readFile(@org.jetbrains.annotations.NotNull() java.lang.String)

  @kotlin.Throws(exceptionClasses = {java.lang.Throwable.class})
  public static final void baz();//  baz()
}
