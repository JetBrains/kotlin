// !WITH_NEW_INFERENCE
// !LANGUAGE: -ShortSyntaxForPropertyGetters
// NI_EXPECTED_FILE

// blockBodyGetter.kt
<!UNSUPPORTED_FEATURE!>val x get() {
    return 1
}<!>

// cantBeInferred.kt
<!UNSUPPORTED_FEATURE!>val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>x1<!> get() = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()<!>
<!UNSUPPORTED_FEATURE!>val y1 get() = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()<!>

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!

// explicitGetterType.kt
<!UNSUPPORTED_FEATURE!>val x2 get(): String = foo()<!>
<!UNSUPPORTED_FEATURE!>val y2 get(): List<Int> = bar()<!>
<!UNSUPPORTED_FEATURE!>val z2 get(): List<Int> {
    return bar()
}<!>

<!MUST_BE_INITIALIZED!>val u<!> get(): String = field

// members.kt
class A {
    <!UNSUPPORTED_FEATURE!>val x get() = 1<!>
    <!UNSUPPORTED_FEATURE!>val y get() = id(1)<!>
    <!UNSUPPORTED_FEATURE!>val y2 get() = id(id(2))<!>
    <!UNSUPPORTED_FEATURE!>val z get() = l("")<!>
    <!UNSUPPORTED_FEATURE!>val z2 get() = l(id(l("")))<!>

    <!UNSUPPORTED_FEATURE!>val <T> T.u get() = id(this)<!>
}
fun <E> id(x: E) = x
fun <E> l(<!UNUSED_PARAMETER!>x<!>: E): List<E> = null!!

// vars
<!UNSUPPORTED_FEATURE!>var x3
    get() = 1
    set(<!UNUSED_PARAMETER!>q<!>) {
    }<!>

// recursive
<!UNSUPPORTED_FEATURE!>val x4 get() = <!NI;DEBUG_INFO_MISSING_UNRESOLVED, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>x4<!><!>

// null as nothing
<!UNSUPPORTED_FEATURE!>val x5 get() = null<!>
<!UNSUPPORTED_FEATURE!>val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y5<!> get() = null!!<!>

// objectExpression.kt
object Outer {
    <!UNSUPPORTED_FEATURE!>private var x
        get() = object : CharSequence {
            override val length: Int
                get() = 0

            override fun get(index: Int): Char {
                return ' '
            }

            override fun subSequence(startIndex: Int, endIndex: Int) = ""

            fun bar() {
            }
        }
        set(q) {
            x = q
        }<!>
}