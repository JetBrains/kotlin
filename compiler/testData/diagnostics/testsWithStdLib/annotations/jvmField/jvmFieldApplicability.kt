// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
<!INAPPLICABLE_JVM_FIELD, WRONG_ANNOTATION_TARGET!>@kotlin.jvm.JvmField<!>
fun foo() {
    <!INAPPLICABLE_JVM_FIELD, WRONG_ANNOTATION_TARGET!>@kotlin.jvm.JvmField<!> val x = "A"
}

<!INAPPLICABLE_JVM_FIELD, WRONG_ANNOTATION_TARGET!>@JvmField<!>
abstract class C : I{

    <!INAPPLICABLE_JVM_FIELD, WRONG_ANNOTATION_TARGET!>@kotlin.jvm.JvmField<!> constructor(s: String) {
    }

    <!INAPPLICABLE_JVM_FIELD, WRONG_ANNOTATION_TARGET!>@kotlin.jvm.JvmField<!> private fun foo(s: String = "OK") {
    }

    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!> val a: String by lazy { "A" }

    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!> open val b: Int = 3

    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!> abstract val c: Int

    <!INAPPLICABLE_JVM_FIELD, INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    val customGetter: String = ""
        get() = field

    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    var customSetter: String = ""
        set(s) {
            field = s
        }

    <!INAPPLICABLE_JVM_FIELD, INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    val noBackingField: String
        get() = "a"

    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    final override val ai = 3

    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    private val private = 3
}

interface I {
    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!> val ai: Int
    <!INAPPLICABLE_JVM_FIELD, INAPPLICABLE_JVM_FIELD!>@JvmField<!> val bi: Int
        get() = 5
}

class G {
    <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
    lateinit var lateInit: String
}

<!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
const val Const = 4

@JvmField
var i = 5

class H {
    companion object {
        @JvmField
        var c = 3
    }
}

interface K {
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        var c = 3
    }
}

object O {
    @JvmField
    val c = 3
}

<!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
private val private = 3