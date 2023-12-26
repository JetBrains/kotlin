// !LANGUAGE: +KotlinFunInterfaceConstructorReference

// DONT_TARGET_EXACT_BACKEND: JVM
//  ^ old JVM BE generates bogus code

// IGNORE_BACKEND: JS
//  ^ Failed: kr is class Function1

// JVM_ABI_K1_K2_DIFF: KT-63861

// WITH_REFLECT

import kotlin.reflect.KFunction

fun interface KRunnable {
    fun run()
}

val kr = ::KRunnable // : KFunction1<() -> Unit, KRunnable>

fun box(): String {
    return if (kr is KFunction<*>)
        "OK"
    else
        "Failed: kr is ${kr::class}"
}
