// See KT-15901

<info>enum</info> class En { A }

fun foo(): Any {
    val en2: Any? = En.A
    if (en2 is En) {
        // Here we had smart casts to En / Any
        // should be always En
        val a: Any = <info descr="Smart cast to En">en2</info>
        return a
    }
    return ""
}