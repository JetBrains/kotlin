public final class MyException /* MyException*/ extends java.lang.Exception {
  public  MyException();//  .ctor()

}

public final class C /* C*/ {
  @kotlin.jvm.Throws(exceptionClasses = {java.io.IOException.class, MyException.class})
  @org.jetbrains.annotations.NotNull()
  public final java.lang.String readFile(@org.jetbrains.annotations.NotNull() java.lang.String);//  readFile(java.lang.String)

  @kotlin.jvm.Throws(exceptionClasses = {java.lang.Exception.class})
  public  C(int);//  .ctor(int)

  @kotlin.jvm.Throws(exceptionClasses = {java.lang.Throwable.class})
  public final void baz();//  baz()

  public  C();//  .ctor()

}
