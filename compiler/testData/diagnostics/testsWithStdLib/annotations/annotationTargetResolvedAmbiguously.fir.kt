import kotlin.annotation.AnnotationTarget.FIELD

object Some {
    @Target(<!ARGUMENT_TYPE_MISMATCH!>AnnotationTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>CLASS<!><!>)
    annotation class Ann

    enum class AnnotationTarget {
        CLASS
    }

    @Target(<!AMBIGUOUS_ANNOTATION_ARGUMENT, ARGUMENT_TYPE_MISMATCH!>FIELD<!>)
    annotation class Ann2

    const val FIELD = ""
}

object SomeMore {
    @Target(<!ARGUMENT_TYPE_MISMATCH!>kotlin.annotation.AnnotationTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>FUNCTION<!><!>)
    annotation class Ann3

    object kotlin {
        object annotation {
            enum class AnnotationTarget {
                FUNCTION
            }
        }
    }
}

abstract class Base {
    annotation class Target(val target: AnnotationTarget)

    enum class AnnotationTarget {
        TYPE
    }
}

class Derived : Base() {
    @<!COMPILER_REQUIRED_ANNOTATION_AMBIGUITY!>Target<!>(<!ARGUMENT_TYPE_MISMATCH!>AnnotationTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>TYPE<!><!>)
    annotation class Ann

    fun foo(x: <!WRONG_ANNOTATION_TARGET!>@Ann<!> String) {}
}
