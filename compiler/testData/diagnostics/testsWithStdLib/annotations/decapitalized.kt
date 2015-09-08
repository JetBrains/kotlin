// !DIAGNOSTICS: -DEPRECATED_UNESCAPED_ANNOTATION -UNUSED_PARAMETER
import kotlin.Extension as extension
import kotlin.jvm.<!DEPRECATED_DECAPITALIZED_ANNOTATION!>strictfp<!> as sfp
class A {
    @<!DEPRECATED_DECAPITALIZED_ANNOTATION!>deprecated<!>("") fun foo() {}
    companion object {
        @<!DEPRECATED_DECAPITALIZED_ANNOTATION!>jvmStatic<!> fun bar() {}
    }

    <!DEPRECATED_DECAPITALIZED_ANNOTATION!>throws<!>(java.lang.RuntimeException::class)
    <!DEPRECATED_DECAPITALIZED_ANNOTATION!>synchronized<!> fun <T> baz() {
        @<!DEPRECATED_DECAPITALIZED_ANNOTATION!>suppress<!>("UNCHECKED_CAST")
        (1 as T)
    }

    kotlin.jvm.<!DEPRECATED_DECAPITALIZED_ANNOTATION!>jvmName<!>("y")
    <!DEPRECATED_DECAPITALIZED_ANNOTATION!>jvmOverloads<!>()
    fun x(x: Int = 1) {}
}

sfp fun bar(
        x: <!DEPRECATED_DECAPITALIZED_ANNOTATION!>deprecated<!>
): @<!DEPRECATED_DECAPITALIZED_ANNOTATION!>extension<!> String.() -> Int = null!!
