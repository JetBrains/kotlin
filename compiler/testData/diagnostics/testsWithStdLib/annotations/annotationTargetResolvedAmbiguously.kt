import kotlin.annotation.AnnotationTarget.FIELD

object Some {
    @Target(<!TYPE_MISMATCH!>AnnotationTarget.CLASS<!>)
    annotation class Ann

    enum class AnnotationTarget {
        CLASS
    }

    @Target(<!TYPE_MISMATCH!>FIELD<!>)
    annotation class Ann2

    const val FIELD = ""
}

object SomeMore {
    @Target(<!TYPE_MISMATCH!>kotlin.annotation.AnnotationTarget.FUNCTION<!>)
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
    @Target(AnnotationTarget.TYPE)
    annotation class Ann

    fun foo(x: <!WRONG_ANNOTATION_TARGET!>@Ann<!> String) {}
}
