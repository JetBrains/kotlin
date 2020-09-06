open class Parameterized<A, C: Cloneable, R: Runnable>

class AC
class CC : Cloneable
class RC : Runnable {
    override fun run() {}
}

fun use(p: Parameterized<AC, CC, RC>) {}

class UsageClass : Parameterized<AC, CC<caret>, RC>()

//Text: (A, <highlight>C : Cloneable</highlight>, R : Runnable), Disabled: false, Strikeout: false, Green: false