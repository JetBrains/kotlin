// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

@Ann(A.B.i) class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(Ann::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.i != 1) return "fail: annotation parameter i should be 1, but was ${ann.i}"
    return "OK"
}

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val i: Int)

class A {
   class B {
      companion object {
        const val i = 1
      }
   }
}
