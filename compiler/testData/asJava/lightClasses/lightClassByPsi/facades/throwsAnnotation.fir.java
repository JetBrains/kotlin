public final class MyException /* MyException*/ extends java.lang.Exception {
  public  MyException();//  .ctor()
}

public final class ThrowsAnnotationKt /* ThrowsAnnotationKt*/ {
  @kotlin.jvm.Throws(exceptionClasses = {java.io.IOException.class, MyException.class})
  @org.jetbrains.annotations.NotNull()
  public static final java.lang.String readFile(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.io.IOException, MyException;//  readFile(java.lang.String)

  @kotlin.jvm.Throws(exceptionClasses = {java.lang.Throwable.class})
  public static final void baz() throws java.lang.Throwable;//  baz()
}
