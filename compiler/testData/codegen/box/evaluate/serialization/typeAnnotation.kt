// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnotation(val str: String)

open class A
interface B
class C : @TypeAnnotation("AClass" + "Anno") A(), @TypeAnnotation("BInterface" + "Anno") B

val a: @TypeAnnotation("Int" + "Anno") Int = 1
var b: @TypeAnnotation("List" + "Anno") List<
        @TypeAnnotation("Pair" + "Anno")Pair<
                @TypeAnnotation("PairInt1" + "Anno") Int,
                @TypeAnnotation("PairInt2" + "Anno") Int
            >
    >? = null

fun foo(a: @TypeAnnotation("String" + "Anno") String): @TypeAnnotation("Any" + "Anno") Any {
    val b : @TypeAnnotation("Double" + "Anno") Double = 1.0
    return b
}

fun <T: @TypeAnnotation("SuperT" + "Anno") Any> bar(a: @TypeAnnotation("T" + "Anno") T) {}

fun example(computeAny: @TypeAnnotation("Fun" + "Anno") () -> Any) {
    val memoizedFoo: @TypeAnnotation("LocalDelegate" + "Anno") Any by lazy(computeAny)
}

typealias Fun = @TypeAnnotation("TypeAlias" + "Anno") (Int, Int) -> Int

fun memberAccess() {
    bar<@TypeAnnotation("Float" + "Anno") Float>(1.0f)
}

val typeOperator = 1L as @TypeAnnotation("Long" + "Anno") Long

fun withVararg(vararg args: @TypeAnnotation("Byte" + "Anno") Byte) {}

fun withAnonymousObject() {
    object {
        fun bar() {
            val a: @TypeAnnotation("InsideObject" + "Anno") A? = null
        }
    }
}

class Outer {
    inner class Inner {
        fun foo(): @TypeAnnotation("InsideInner" + "Anno") Int = 0
    }
}

fun functionWithLambda(action: (Int, String) -> Any) {
    action(0, "")
}

fun lambda() {
    functionWithLambda { integer: @TypeAnnotation("InsideLambdaInt" + "Anno") Int, string ->
        val a: @TypeAnnotation("InsideLambda" + "Anno") Int = 0
        a
    }
}

val inProjection: MutableList<in @TypeAnnotation("InProjection" + "Anno") String> = mutableListOf()
val outProjection: MutableList<out @TypeAnnotation("OutProjection" + "Anno") String> = mutableListOf()

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return "OK"
}
