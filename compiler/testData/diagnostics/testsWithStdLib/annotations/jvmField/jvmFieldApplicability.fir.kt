// !LANGUAGE: +NestedClassesInAnnotations +InlineClasses -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

@kotlin.jvm.JvmField
fun foo() {
    @kotlin.jvm.JvmField val x = "A"
}

annotation class DemoAnnotation

@JvmField
abstract class C : I{

    @kotlin.jvm.JvmField constructor(s: String) {
    }

    @kotlin.jvm.JvmField private fun foo(s: String = "OK") {
    }

    @JvmField val a: String by lazy { "A" }

    @JvmField open val b: Int = 3

    @JvmField abstract val c: Int

    @JvmField
    val customGetter: String = ""
        get() = field

    @JvmField
    val explicitDefaultGetter: String = ""
        get

    @JvmField
    var explicitDefaultSetter: String = ""
        set

    @JvmField
    val explicitDefaultAnnotatedGetter: String = ""
        @DemoAnnotation get

    @JvmField
    var explicitDefaultAnnotatedSetter: String = ""
        @DemoAnnotation set

    @JvmField
    var customSetter: String = ""
        set(s) {
            field = s
        }

    @JvmField
    val noBackingField: String
        get() = "a"

    @JvmField
    final override val ai = 3

    @JvmField
    private val private = 3
}

interface I {
    @JvmField val ai: Int
    @JvmField val bi: Int
        get() = 5
}

class G {
    @JvmField
    lateinit var lateInit: String

    @delegate:JvmField
    val s: String by lazy { "s" }
}

@JvmField
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

    val i: Int
    val j: Int

    companion object {
        @JvmField
        var c = 3

        var x = 3
    }
}

class KK : K {
    @JvmField
    override val i: Int = 0
    @JvmField
    override final val j: Int = 0
}

open class KKK : K {
    @JvmField
    override val i: Int = 0
    @JvmField
    override final val j: Int = 0
}

annotation class L {
    companion object {
        @JvmField
        var c = 3
    }
}

object O {
    @JvmField
    val c = 3
}

@JvmField
private val private = 3

inline class Foo(val x: Int)

object IObject {
    @JvmField
    val c: Foo = Foo(42)

    @JvmField
    val u = 42u

    @JvmField
    private val r: Result<Int> = TODO()
}
