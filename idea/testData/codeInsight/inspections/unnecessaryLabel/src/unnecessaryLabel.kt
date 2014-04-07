fun unnecessaryLabel_break() {
    @loop while (true) {
        break@loop // should warn
    }
}
fun unnecessaryLabel_continue() {
    @loop do {
        continue@loop // should warn
    } while (false)
}

class A() {
    fun f() = 43
}

class UnnecessaryLabel_super() : A {
    fun f() = super@UnnecessaryLabel_super.f() - 1 // should warn
}

class UnnecessaryLabel_this() {
    fun g(): Any {
        return this@UnnecessaryLabel_this // should warn
    }
}
