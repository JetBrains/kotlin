// FILE: Simple.kt
package test

annotation class Simple(val i: Int) {
    companion object {
        const val CONST1 = 1
        const val CONST2 = 2
    }
}

// FILE: Qualified.kt
import test.Simple

@Simple(test.Simple.Companion.CONST1)
class Qualified

// FILE: Sum.kt
import test.Simple

@Simple(test.Simple.Companion.CONST1 + Simple.CONST2)
class Sum

// FILE: Negative.kt
import test.Simple

@Simple(-test.Simple.Companion.CONST1)
class Negative

// FILE: Negative2.kt
import test.Simple

@Simple(- - -test.Simple.Companion.CONST1)
class Negative2

// FILE: Positive.kt
import test.Simple

@Simple(-(-test.Simple.Companion.CONST1))
class Positive
