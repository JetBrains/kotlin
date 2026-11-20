// IGNORE_FIR
class Ctx

class B {
    context(ctx: Ctx)
    operator fun get(i: Int): Int = 0
    operator fun set(i: Int, value: Int) {}
}

context(ctx: Ctx)
fun test(b: B) {
    <expr>b[0]</expr> += 1
}
