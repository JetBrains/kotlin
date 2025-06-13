// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-67314
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<E>

interface B : A<Int>
interface C : A<Long>

fun <F> Controller<F>.baz(a: A<F>, x: () -> Unit) {}

fun <T> bar(a: A<T>, w: T) {
    generate {
        if (a is B) {
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>) {}
        }

        yield(1)
    }

    generate {
        if (a is B) {
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>) {}
        }

        yield(<!TYPE_MISMATCH, TYPE_MISMATCH!>w<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
integerLiteral, interfaceDeclaration, intersectionType, isExpression, lambdaLiteral, nullableType, smartcast, suspend,
typeParameter, typeWithExtension */
