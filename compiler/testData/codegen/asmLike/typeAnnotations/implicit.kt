// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// WITH_STDLIB

// FIR_DIFFERENCE
// With FIR, the backend generates lambdas via invokedynamic by default.

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeAnnSource

class Kotlin {

    fun foo2(): @TypeAnn("2") @TypeAnnBinary @TypeAnnSource String {
        return "OK"
    }

    fun foo3() = foo2()

    fun foo4() = { foo2() }()

    fun foo5() {
        val lambda = @JvmSerializableLambda { foo2() }
        lambda()
    }

    fun foo6() {
        val indyLambda = { foo2() }
        indyLambda()
    }
}
