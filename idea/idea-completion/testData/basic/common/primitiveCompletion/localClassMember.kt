package test

class T

fun topLevelFun(param: T) {
    val firstVariable = T()

    class LocalClass(constructorParam: T) {
        val localClassField = constructorParam

        fun localClassMethod(localParam: T) {
            <caret>
        }
    }

    val secondVariable = T()
}

// EXIST: firstVariable, param, localClassField, localParam
// ABSENT: secondVariable
// FIR_COMPARISON