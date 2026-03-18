// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions, +ForbidUselessTypeArgumentsIn25
// COMPARE_WITH_LIGHT_TREE
// FILE: C.kt
class C {
    companion {
        fun foo() {
        }

        val bar = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
        val baz get() = 2
    }

    fun test() {
        foo()
        bar
        baz
    }
}

typealias TA1 = C
typealias TA2<T> = C

fun test() {
    C.foo()
    C.bar
    C.baz

    TA1.foo()
    TA1.bar
    TA1.baz

    TA2.foo()
    TA2.bar
    TA2.baz

    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.foo()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.bar
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.baz
}

// FILE: other.kt
import C.foo
import C.bar
import C.baz

<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{PSI}!>import <!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{LT}!>TA1<!>.foo as fooTa1<!>
<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{PSI}!>import <!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{LT}!>TA1<!>.bar as barTa1<!>
<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{PSI}!>import <!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{LT}!>TA1<!>.baz as bazTa1<!>

<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{PSI}!>import <!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{LT}!>TA2<!>.foo as fooTa2<!>
<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{PSI}!>import <!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{LT}!>TA2<!>.bar as barTa2<!>
<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{PSI}!>import <!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR{LT}!>TA2<!>.baz as bazTa2<!>

fun test2() {
    foo()
    bar
    baz

    fooTa1()
    barTa1
    bazTa1

    fooTa2()
    barTa2
    bazTa2
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, propertyDeclaration */
