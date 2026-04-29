// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// DUMP_INFERENCE_LOGS: MARKDOWN
// LANGUAGE: +CallCompletionRefinementsFor25
// ISSUE: KT-86042

interface Out<out T1, out T2>
fun <R1, I, R2> foo(r1: R1, i: I, y: (I) -> R1, z: (I) -> R2): Out<Base, R2> = TODO()

fun bar(x: Out<Base, String>) {}
@JvmName("bar2")
fun bar(x: Out<Base, Int>) {}

interface Base
interface Derived : Base

fun test(base: Base, derived: Derived) {
    foo(
        derived, "",
        { base },
        { "" }
    )

    bar(
        foo(
            derived, "",
            { base },
            { "" }
        )
    )
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, nullableType, out,
stringLiteral, typeParameter */
