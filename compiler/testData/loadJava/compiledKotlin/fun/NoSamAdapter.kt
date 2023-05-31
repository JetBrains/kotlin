// TARGET_BACKEND: JVM
package test

public interface TaskObject {
    fun foo(r: Runnable)
}

fun foo(r: Runnable) {
}
