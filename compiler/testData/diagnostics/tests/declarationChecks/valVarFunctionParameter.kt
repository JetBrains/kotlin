fun f(
        <!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> a: Int,
        <!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> b: Int,
        c: Int,
        vararg <!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> d: Int,
        vararg <!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> e: Int,
        vararg f: Int
) {
    
    
    a + b + c + d[0] + e[0] + f[0] // to avoid 'unused parameter'
}