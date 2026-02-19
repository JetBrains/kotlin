// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
abstract class Checker<StateT>

class ToolchainPanel {
    fun <ItemT> addVersionChecker(item: ItemT) {
        class MyState(val selectedItem: ItemT?)
        object : Checker<MyState>() {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, localClass, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
