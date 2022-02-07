fun test_1() {
    a?.f()
    a?.x += f()
    a?.f[i]
    a?.f[i] = g()
    a?.f[i] += g()
    a?.i++
}

fun test_2() {
    a?.f() /* a?.run { this.f() } */
    a?.x += f() /* a?.run { this.x += f() } */
    a?.f[i] /* a?.run { this.f[i] } */
    a?.f[i] = g() /* a?.run { this.f[i]  = g() } */
    a?.f[i] += g() /* a?.run { this.f[i] += g() } */
    a?.i++ /* a?.run { this.i++ } */
}
