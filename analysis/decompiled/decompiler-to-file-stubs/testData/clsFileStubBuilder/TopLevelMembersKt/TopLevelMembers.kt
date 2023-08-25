// Issue: KTIJ-26761
// KNM_K2_IGNORE

package foo.TopLevelMembers

fun funWithBlockBody() {
}

private fun funWithExprBody() = 3

private fun funWithParams(c: Int) {
}

public val immutable: Double = 0.0

public var mutable: Float = 0.0f

public val String.ext: String
    get() = this

public fun Int.ext(i: Int = 3): Int = this + i

private fun funWithVarargParam(c: Int, vararg v: Int) {
}

private fun probablyNothing(): Nothing = throw IllegalStateException()

private val certainlyNothing: kotlin.Nothing = throw IllegalStateException()

private typealias Alias<E> = (E) -> E

class Nothing

@Target(AnnotationTarget.FIELD)
annotation class A

class D

public val D.Main: D? get() = null

@A internal val Main: D? = null
