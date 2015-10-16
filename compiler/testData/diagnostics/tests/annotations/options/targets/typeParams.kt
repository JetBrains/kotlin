//!DIAGNOSTICS: -UNUSED_PARAMETER

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class A

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class B(val i: Int = 12)


fun <@A @B(3) T> topFun() = 12

class Class1 {
    fun <@A @B(3)T> method() = 12

    fun foo() {
        fun <@A @B(3) T> innerFun() = 12
    }
}

val <@A @B(3) T> T.topProp: Int get() = 12

class Class2 {
    val <@A @B(3) T> T.field: Int get() = 12
}


<!WRONG_ANNOTATION_TARGET!>@A<!> fun foo() {}
<!WRONG_ANNOTATION_TARGET!>@A<!> class D
fun foo(i: <!WRONG_ANNOTATION_TARGET!>@A<!> Int) {
    <!WRONG_ANNOTATION_TARGET!>@A<!> val <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 1
}
fun <T> test(t: <!WRONG_ANNOTATION_TARGET!>@A<!> T): T = t


@Target(AnnotationTarget.TYPE)
internal annotation class C

fun <<!WRONG_ANNOTATION_TARGET!>@C<!> T> test2(t: T): T = t