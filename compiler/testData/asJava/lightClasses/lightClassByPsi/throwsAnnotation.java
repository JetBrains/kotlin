public final class C /* C*/ {
  @kotlin.jvm.Throws(exceptionClasses = {java.io.IOException.class, MyException.class})
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String readFile(@org.jetbrains.annotations.NotNull() java.lang.String) throws java.io.IOException, MyException;//  readFile(java.lang.String)

  @kotlin.jvm.Throws(exceptionClasses = {java.lang.Exception.class})
  public  C() throws java.lang.Exception;//  .ctor()

  @kotlin.jvm.Throws(exceptionClasses = {java.lang.Exception.class})
  public  C(int) throws java.lang.Exception;//  .ctor(int)

  @kotlin.jvm.Throws(exceptionClasses = {kotlin.Throwable.class})
  public final void baz() throws java.lang.Throwable;//  baz()
}

public final class MyException /* MyException*/ extends java.lang.Exception {
  public  MyException();//  .ctor()
}
