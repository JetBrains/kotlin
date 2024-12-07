// TARGET_BACKEND: JVM
package test

val <P> P.anotherJavaClass: java.lang.Class<P>
    get() = throw Exception()
