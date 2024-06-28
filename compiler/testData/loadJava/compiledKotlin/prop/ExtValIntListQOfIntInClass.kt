// TARGET_BACKEND: JVM
package test

class ExtValInClass {
    val Int.asas: java.util.List<Int>?
        get() = throw Exception()
}
