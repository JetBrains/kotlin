fun f(
        val a: Int,
        var b: Int,
        c: Int,
        <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> var d: Int,
        <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> val e: Int,
        <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> f: Int
) {
    
    
    a + b + c + d[0] + e[0] + f[0] // to avoid 'unused parameter'
}
