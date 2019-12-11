fun f(
        val a: Int,
        var b: Int,
        c: Int,
        vararg var d: Int,
        vararg val e: Int,
        vararg f: Int
) {
    
    
    a + b + c + d[0] + e[0] + f[0] // to avoid 'unused parameter'
}