// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89779
// DIAGNOSTICS: -UNUSED_PARAMETER
// FIR_DUMP
enum class MyEnum { X, Y }

fun foo(m: MyEnum) {} // (1)

fun bar(m: MyEnum) {} // (1)
fun bar(m: Any) {} // (2)

class A {
    fun member(m: Any) {} // (2)
}

fun member(m: MyEnum) {} // (1)

fun main(a: A) {
    fun foo(m: Any) {} // (2)

    foo(X)
    foo(MyEnum.X)

    bar(X)
    bar(MyEnum.X)

    with(a) {
        member(X)
        member(MyEnum.X)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, lambdaLiteral, localFunction */
