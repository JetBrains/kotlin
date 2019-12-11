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


@A fun foo() {}
@A class D
fun foo(i: @A Int) {
    @A val i = 1
}
fun <T> test(t: @A T): T = t


@Target(AnnotationTarget.TYPE)
internal annotation class C

fun <@C T> test2(t: T): T = t