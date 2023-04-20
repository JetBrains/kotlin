// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnotation(val str: String)

open class A
interface B
class C : @TypeAnnotation("AClass" <!EVALUATED("AClassAnno")!>+ "Anno"<!>) A(), @TypeAnnotation("BInterface" <!EVALUATED("BInterfaceAnno")!>+ "Anno"<!>) B

val a: @TypeAnnotation("Int" <!EVALUATED("IntAnno")!>+ "Anno"<!>) Int = 1
var b: @TypeAnnotation("List" <!EVALUATED("ListAnno")!>+ "Anno"<!>) List<
        @TypeAnnotation("Pair" <!EVALUATED("PairAnno")!>+ "Anno"<!>)Pair<
                @TypeAnnotation("PairInt1" <!EVALUATED("PairInt1Anno")!>+ "Anno"<!>) Int,
                @TypeAnnotation("PairInt2" <!EVALUATED("PairInt2Anno")!>+ "Anno"<!>) Int
            >
    >? = null

fun foo(a: @TypeAnnotation("String" <!EVALUATED("StringAnno")!>+ "Anno"<!>) String): @TypeAnnotation("Any" <!EVALUATED("AnyAnno")!>+ "Anno"<!>) Any {
    val b : @TypeAnnotation("Double" <!EVALUATED("DoubleAnno")!>+ "Anno"<!>) Double = 1.0
    return b
}

fun <T: @TypeAnnotation("SuperT" <!EVALUATED("SuperTAnno")!>+ "Anno"<!>) Any> bar(a: @TypeAnnotation("T" <!EVALUATED("TAnno")!>+ "Anno"<!>) T) {}

fun example(computeAny: @TypeAnnotation("Fun" <!EVALUATED("FunAnno")!>+ "Anno"<!>) () -> Any) {
    val memoizedFoo: @TypeAnnotation("LocalDelegate" <!EVALUATED("LocalDelegateAnno")!>+ "Anno"<!>) Any by lazy(computeAny)
}

typealias Fun = @TypeAnnotation("TypeAlias" <!EVALUATED("TypeAliasAnno")!>+ "Anno"<!>) (Int, Int) -> Int

fun memberAccess() {
    bar<@TypeAnnotation("Float" <!EVALUATED("FloatAnno")!>+ "Anno"<!>) Float>(1.0f)
}

val typeOperator = 1L as @TypeAnnotation("Long" <!EVALUATED("LongAnno")!>+ "Anno"<!>) Long

fun withVararg(vararg args: @TypeAnnotation("Byte" <!EVALUATED("ByteAnno")!>+ "Anno"<!>) Byte) {}

fun withAnonymousObject() {
    object {
        fun bar() {
            val a: @TypeAnnotation("InsideObject" <!EVALUATED("InsideObjectAnno")!>+ "Anno"<!>) A? = null
        }
    }
}

class Outer {
    inner class Inner {
        fun foo(): @TypeAnnotation("InsideInner" <!EVALUATED("InsideInnerAnno")!>+ "Anno"<!>) Int = 0
    }
}

fun functionWithLambda(action: (Int, String) -> Any) {
    action(0, "")
}

fun lambda() {
    functionWithLambda { integer: @TypeAnnotation("InsideLambdaInt" <!EVALUATED("InsideLambdaIntAnno")!>+ "Anno"<!>) Int, string ->
        val a: @TypeAnnotation("InsideLambda" <!EVALUATED("InsideLambdaAnno")!>+ "Anno"<!>) Int = 0
        a
    }
}

val inProjection: MutableList<in @TypeAnnotation("InProjection" <!EVALUATED("InProjectionAnno")!>+ "Anno"<!>) String> = mutableListOf()
val outProjection: MutableList<out @TypeAnnotation("OutProjection" <!EVALUATED("OutProjectionAnno")!>+ "Anno"<!>) String> = mutableListOf()

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
