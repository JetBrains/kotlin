import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Ann(A.B.i) class MyClass

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann>())
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.i != 1) return "fail: annotation parameter i should be 1, but was ${ann.i}"
    return "OK"
}

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(val i: Int)

class A {
   class B {
      default object {
        val i = 1
      }
   }
}
