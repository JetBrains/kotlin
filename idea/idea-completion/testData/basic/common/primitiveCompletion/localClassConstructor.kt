package test

class T

fun topLevelFun(param: T) {
    val firstVariable = T()

    class LocalClass(constructorParam: T) {
        val localClassField = constructorParam

        init {
            <caret>
        }
    }

    val secondVariable = T()
}

// EXIST: firstVariable, param, constructorParam, localClassField
// ABSENT: secondVariable
// FIR_COMPARISON