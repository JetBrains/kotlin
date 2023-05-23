// TARGET_BACKEND: WASM

// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 18_621
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs 5_328

// Muting because K2 DCE code size was less than expected (17377)
// IGNORE_BACKEND_K2: WASM

object Simple

object SimpleWithConstVal {
   const val MAX = 4
}

object SimpleWithPureProperty {
    val text = "Hello"
}

object SimpleWithPropertyInitializedDurintInit {
    val text: String
    init {
        text = "Hello"
    }
}

object SimpleWithFunctionsOnly {
    fun foo() = "Foo"
    fun bar() = "Bar"
}

object SimpleWithDifferentMembers {
    val foo = "Foo"
    fun bar() = "Bar"
}

interface Callable {
    fun call(): String
}

object SimpleWithInterface : Callable {
    override fun call() = "OK"
}

object UsedGetFieldInside {
    val anotherText = SimpleWithPureProperty.text
}

class ClassWithCompanion {
   companion object
}

class ClassWithCompanionWithConst {
    companion object {
        const val MAX = 5
    }
}

fun box(): String {
    if (Simple !is Any) return "Fail simple object"
    if (SimpleWithConstVal.MAX != 4) return "Fail simple case with const val"
    if (SimpleWithPureProperty.text != "Hello") return "Fail simple case with pure property"
    if (SimpleWithPropertyInitializedDurintInit.text != "Hello") return "Fail simple case with pure property initialized inside init block"
    if (SimpleWithFunctionsOnly.foo() != "Foo" || SimpleWithFunctionsOnly.bar() != "Bar") return "Fail simple case with functions only"
    if (SimpleWithInterface.call() != "OK") return "Fail simple case with interface implementing"
    if (UsedGetFieldInside.anotherText != "Hello") return "Fail object which used another object inside its initialization block"
    if (ClassWithCompanion.Companion !is Any) return "Fail simple companion object"
    if (ClassWithCompanionWithConst.MAX != 5) return "Fail simple companion object with const val"
    SimpleWithDifferentMembers
    return "OK"
}