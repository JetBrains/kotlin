// SUGGESTED_NAMES: s, getA
// PARAM_TYPES: kotlin.String
// PARAM_TYPES: kotlin.String, kotlin.Comparable<kotlin.String>, kotlin.CharSequence, java.io.Serializable, kotlin.Any
// PARAM_TYPES: kotlin.String, kotlin.Comparable<kotlin.String>, kotlin.CharSequence, java.io.Serializable, kotlin.Any
// PARAM_DESCRIPTOR: local final fun kotlin.String.<anonymous>(): kotlin.Unit defined in Foo.foo.<anonymous>.<anonymous>, local final fun kotlin.String.<anonymous>(): kotlin.Unit defined in Foo.foo.<anonymous>, local final fun kotlin.String.<anonymous>(): kotlin.Unit defined in Foo.foo
class Foo {
    fun foo() {
        block("a") a@ {
            block("b") b@ {
                block("c") c@ {
                    val a = <selection>this@c + this@b + this@a + this + this@Foo.toString()</selection>
                }
            }
        }
    }
}

private inline fun <T> block(t: T, block: T.() -> Unit) {
    t.block()
}