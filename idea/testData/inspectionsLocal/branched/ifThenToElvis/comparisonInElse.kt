// See KT-15227
class My(val local: Boolean)

class Your(val my: My?, val parent: Any?)

fun foo(your: Your): Boolean {
    val my = your.my
    return <caret>if (my != null) my.local else your.parent != null
}