package test2

<info descr="null">enum</info> class En { A, B, С }

fun main(args: Array<String>) {
    val en2: Any? = En.A
    if (en2 is En) {
        when (<info descr="Smart cast to test2.En">en2</info>) {
            En.A -> {}
            En.B -> {}
            En.С -> {}
        }
    }
}
