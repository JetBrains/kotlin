// FIX: Convert to run { ... }
// WITH_RUNTIME
class C {
    fun f4() = {<caret>
        "single-expression function which returns lambda"
    }
}