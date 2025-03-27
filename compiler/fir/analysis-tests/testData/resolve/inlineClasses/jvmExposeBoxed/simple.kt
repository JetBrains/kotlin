// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:JvmExposeBoxed<!>

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
class Foo
<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)<!> constructor()
{
    @JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
    companion object {

    }
}

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
class Obj

@JvmExposeBoxed
@JvmInline
value class IC(val s: String)

<!WRONG_ANNOTATION_TARGET!>@JvmExposeBoxed<!>
val ic: IC = TODO()

<!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@get:JvmExposeBoxed<!>
val icgetter: IC = TODO()

val icgetter2: IC
    <!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@JvmExposeBoxed<!> get() = TODO()

@set:JvmExposeBoxed
var icsetter: IC = TODO()

var icsetter2: IC
    get() = TODO()
    @JvmExposeBoxed set(value) {}

@get:JvmExposeBoxed("foo")
val icic: IC = TODO()

<!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@JvmExposeBoxed<!>
fun foo(): IC = TODO()

@JvmExposeBoxed
fun bar(ic: IC) {}

@JvmExposeBoxed
fun foo(result: Result<Any>) {}

class C {
    @JvmExposeBoxed
    fun foo(): IC = TODO()

    @JvmExposeBoxed
    fun foo(result: Result<Any>) {}

    <!WRONG_ANNOTATION_TARGET!>@JvmExposeBoxed<!>
    val ic: IC = TODO()

    @get:JvmExposeBoxed
    val icgetter: IC = TODO()

    val icgetter2: IC
        @JvmExposeBoxed get() = TODO()

    @set:JvmExposeBoxed
    var icsetter: IC = TODO()

    var icsetter2: IC
        get() = TODO()
        @JvmExposeBoxed set(value) {}
}

<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
fun Int.foo(i: Int) {}

@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>"..."<!>)
fun todo(ic: IC) {}

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED!>@JvmExposeBoxed<!>
inline fun <reified T> inlineMe(ic: IC) {}

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_REIFIED!>@get:JvmExposeBoxed("foo")<!>
inline val <reified T> T.bar: IC
        get() = TODO()

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME!>"same"<!>)
fun same(): IC = TODO()

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND!>@JvmExposeBoxed<!>
suspend fun suspendMe(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME!>"foo" + "bar"<!>)
fun foobar(): IC = TODO()

@JvmExposeBoxed
class WithIC {
    fun acceptsIC(ic: IC) {}

    fun returnsIC(): IC = TODO()

    val ic: IC = TODO()

    val icgetter: IC = TODO()

    val icgetter2: IC
        get() = TODO()

    var icsetter: IC = TODO()

    var icsetter2: IC
        get() = TODO()
        set(value) {}
}

abstract class Abstract {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT!>@JvmExposeBoxed<!>
    open fun openIC(ic: IC) {}

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT!>@JvmExposeBoxed<!>
    abstract fun abstractIC(ic: IC)
}

<!JVM_EXPOSE_BOXED_ON_INTERFACE!>@JvmExposeBoxed<!>
interface Interface {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT!>@JvmExposeBoxed<!>
    fun foo(ic: IC) {}

    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT!>@JvmExposeBoxed<!>
    fun bar(ic: IC)
}

class Class: Interface {
    @JvmExposeBoxed
    override fun bar(ic: IC) {}
}

@JvmSynthetic
<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC!>@JvmExposeBoxed<!>
fun syntheticFun(ic: IC) {}

fun withLocal() {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS, USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
    fun local() {}
}
