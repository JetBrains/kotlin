// LIBRARY_PLATFORMS: JVM

@JvmInline
value class Some(val value: String)

class RegularClass {
    fun <T : Some> classFunInParameter(t: T) {}
    fun <T : Some> classFunInReturn(): T = TODO()
    fun <T : Some> T.classFunInExtension() {}

    var <T : Some> T.classPropInExtension: Int
        get() = 1
        set(value) {}

    @JvmName("specialName")
    fun <T : Some> classFunWithJvmName(t: T) {}
}

interface RegularInterface {
    fun <T : Some> interfaceFunInParameter(t: T)
    fun <T : Some> interfaceFunInReturn(): T
}

class ResultAsUpperBound {
    fun <T : Result<String>> funInParameter(t: T) {}
    fun <T : Result<String>> funInReturn(): T = TODO()
}

fun <T : Some> topLevelFunInParameter(t: T) {}
fun <T : Some> topLevelFunInReturn(): T = TODO()

// DECLARATIONS_NO_LIGHT_ELEMENTS: RegularClass.class[classFunInExtension;classFunInParameter;classFunInReturn;classPropInExtension], RegularInterface.class[interfaceFunInParameter;interfaceFunInReturn], ResultAsUpperBound.class[funInReturn], ValueClassAsUpperBoundKt.class[topLevelFunInParameter]
// LIGHT_ELEMENTS_NO_DECLARATION: RegularClass.class[classFunInExtension-5lyY9Q4;classFunInParameter-5lyY9Q4;classFunInReturn-YO-7n-0;getClassPropInExtension-5lyY9Q4;setClassPropInExtension-54afNMI], RegularInterface.class[interfaceFunInParameter-5lyY9Q4;interfaceFunInReturn-YO-7n-0], ResultAsUpperBound.class[funInReturn-d1pmJ48], Some.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], ValueClassAsUpperBoundKt.class[topLevelFunInParameter-5lyY9Q4]
