// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: simple.kt
package simple

@RequiresOptIn(level = WARNING)
@Target(CLASS, TYPEALIAS)
annotation class Experimental

@Experimental
fun interface X {
    fun x()
}

typealias Y = <!OPT_IN_USAGE!>X<!>

fun interface Z {
    fun z()
}

@Experimental
typealias T = Z

@Experimental
fun interface P {
    fun p()
}

@OptIn(Experimental::class)
typealias Q = P

fun test() {
    <!OPT_IN_USAGE!>X<!> { }
    Y { }
    Z { }
    <!OPT_IN_USAGE!>T<!> { }
    <!OPT_IN_USAGE!>P<!> { }
    Q { }
}

// FILE: kt92034.kt
package problem

@RequiresOptIn(level = WARNING)
annotation class Experimental

@Experimental
object E

fun interface Foo<T> {
    fun foo()
}

@OptIn(Experimental::class)
typealias P = Foo<E>

class Bar<T>

@OptIn(Experimental::class)
typealias Q = Bar<E>

fun test() {
    Q()
    <!OPT_IN_USAGE!>P<!> { }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
typeAliasDeclaration */
