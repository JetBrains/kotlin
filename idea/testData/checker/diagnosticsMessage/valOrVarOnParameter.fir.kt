// WITH_RUNTIME

class A {
    constructor(<warning descr="[VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER] 'val' on secondary constructor parameter is not allowed">val</warning> <warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Int) {
        for (<warning descr="[VAL_OR_VAR_ON_LOOP_PARAMETER] 'val' on loop parameter is not allowed">val</warning> z in 1..4) {}
    }

    fun foo(<warning descr="[VAL_OR_VAR_ON_FUN_PARAMETER] 'var' on function parameter is not allowed">var</warning> <warning descr="[UNUSED_PARAMETER] Parameter 'y' is never used">y</warning>: Int) {
        try {
            for (<warning descr="[VAL_OR_VAR_ON_LOOP_PARAMETER] 'var' on loop parameter is not allowed">var</warning> (<warning descr="[UNUSED_VARIABLE] Variable 'i' is never used">i</warning>, <warning descr="[UNUSED_VARIABLE] Variable 'j' is never used">j</warning>) in listOf(1 to 4)) {}
        } catch (<warning descr="[VAL_OR_VAR_ON_CATCH_PARAMETER] 'val' on catch parameter is not allowed">val</warning> e: Exception) {
        }
    }
}