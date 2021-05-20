// WITH_RUNTIME

class A {
    constructor(<error descr="[VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER] 'val' on secondary constructor parameter is not allowed">val</error> <warning descr="[UNUSED_PARAMETER] Parameter 'x' is never used">x</warning>: Int) {
        for (<error descr="[VAL_OR_VAR_ON_LOOP_PARAMETER] 'val' on loop parameter is not allowed">val</error> z in 1..4) {}
    }

    fun foo(<error descr="[VAL_OR_VAR_ON_FUN_PARAMETER] 'var' on function parameter is not allowed">var</error> <warning descr="[UNUSED_PARAMETER] Parameter 'y' is never used">y</warning>: Int) {
        try {
            for (<error descr="[VAL_OR_VAR_ON_LOOP_PARAMETER] 'var' on loop parameter is not allowed">var</error> (<warning descr="[UNUSED_VARIABLE] Variable 'i' is never used">i</warning>, <warning descr="[UNUSED_VARIABLE] Variable 'j' is never used">j</warning>) in listOf(1 to 4)) {}
        } catch (<error descr="[VAL_OR_VAR_ON_CATCH_PARAMETER] 'val' on catch parameter is not allowed">val</error> e: Exception) {
        }
    }
}