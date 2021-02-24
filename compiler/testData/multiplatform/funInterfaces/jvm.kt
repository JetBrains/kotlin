// ADDITIONAL_COMPILER_ARGUMENTS: -Xnew-inference

package jvm

fun interface KRunnable {
    fun invoke(): String
}

fun foo(k: KRunnable) = k.invoke()

fun test() {
    foo { "OK" }
    foo(KRunnable { "OK " })
}