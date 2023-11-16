// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

// TODO: must uncomment some tests when figure out how to pass `FirFile` to const provider

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnotation(val str: String)

open class A
interface B
//class C : @TypeAnnotation("AClass" + "Anno") A(), @TypeAnnotation("BInterface" + "Anno") B

val a: @TypeAnnotation(<!EVALUATED("IntAnno")!>"Int" + "Anno"<!>) Int = 1
var b: @TypeAnnotation(<!EVALUATED("ListAnno")!>"List" + "Anno"<!>) List<
        @TypeAnnotation(<!EVALUATED("PairAnno")!>"Pair" + "Anno"<!>)Pair<
                @TypeAnnotation(<!EVALUATED("PairInt1Anno")!>"PairInt1" + "Anno"<!>) Int,
                @TypeAnnotation(<!EVALUATED("PairInt2Anno")!>"PairInt2" + "Anno"<!>) Int
            >
    >? = null

fun foo(a: @TypeAnnotation(<!EVALUATED("StringAnno")!>"String" + "Anno"<!>) String): @TypeAnnotation(<!EVALUATED("AnyAnno")!>"Any" + "Anno"<!>) Any {
    val b : @TypeAnnotation(<!EVALUATED("DoubleAnno")!>"Double" + "Anno"<!>) Double = 1.0
    return b
}

fun <T: @TypeAnnotation(<!EVALUATED("SuperTAnno")!>"SuperT" + "Anno"<!>) Any> bar(a: @TypeAnnotation(<!EVALUATED("TAnno")!>"T" + "Anno"<!>) T) {}

fun example(computeAny: @TypeAnnotation(<!EVALUATED("FunAnno")!>"Fun" + "Anno"<!>) () -> Any) {
//    val memoizedFoo: @TypeAnnotation("LocalDelegate" + "Anno") Any by lazy(computeAny)
}

typealias Fun = @TypeAnnotation(<!EVALUATED("TypeAliasAnno")!>"TypeAlias" + "Anno"<!>) (Int, Int) -> Int

fun memberAccess() {
    bar<@TypeAnnotation(<!EVALUATED("FloatAnno")!>"Float" + "Anno"<!>) Float>(1.0f)
}

val typeOperator = 1L as @TypeAnnotation(<!EVALUATED("LongAnno")!>"Long" + "Anno"<!>) Long

fun withVararg(vararg args: @TypeAnnotation(<!EVALUATED("ByteAnno")!>"Byte" + "Anno"<!>) Byte) {}

fun withAnonymousObject() {
    object {
        fun bar() {
            val a: @TypeAnnotation(<!EVALUATED("InsideObjectAnno")!>"InsideObject" + "Anno"<!>) A? = null
        }
    }
}

class Outer {
    inner class Inner {
//        fun foo(): @TypeAnnotation("InsideInner" + "Anno") Int = 0
    }
}

fun functionWithLambda(action: (Int, String) -> Any) {
    action(0, "")
}

//fun lambda() {
//    functionWithLambda { integer: @TypeAnnotation("InsideLambdaInt" + "Anno") Int, string ->
//        val a: @TypeAnnotation("InsideLambda" + "Anno") Int = 0
//        a
//    }
//}

val inProjection: MutableList<in @TypeAnnotation(<!EVALUATED("InProjectionAnno")!>"InProjection" + "Anno"<!>) String> = mutableListOf()
val outProjection: MutableList<out @TypeAnnotation(<!EVALUATED("OutProjectionAnno")!>"OutProjection" + "Anno"<!>) String> = mutableListOf()

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
