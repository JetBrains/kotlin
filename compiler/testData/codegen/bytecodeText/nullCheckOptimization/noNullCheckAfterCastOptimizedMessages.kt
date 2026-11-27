// LANGUAGE: +JvmOptimizedNullCastMessages
// TARGET_BACKEND: JVM

interface A {
    fun foo()
}

class B : A {
    override fun foo() {
    }
}

fun test1() {
    val b = B()
    (b as A).foo()
}

fun test2() {
    val b = getB()
    (b as A).foo()
}

fun test3() {
    val b = getB()
    b.foo()
}

fun getB(): B = B()

// 0 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullInCast \(Ljava/lang/Object;Ljava/lang/Class;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
