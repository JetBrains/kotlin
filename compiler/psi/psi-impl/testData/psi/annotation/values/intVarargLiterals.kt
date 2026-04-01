// FILE: VarArg.kt
annotation class VarArg(vararg val v: Int) {
    companion object {
        const val CONSTANT = 3
    }
}

// FILE: One.kt
@VarArg(1)
class One

// FILE: Two.kt
@VarArg(1, 2)
class Two

// FILE: Three.kt
@VarArg(1, 2, VarArg.CONSTANT)
class Three

// FILE: Spread.kt
@VarArg(*[1, 2, VarArg.CONSTANT, 4])
class Spread
