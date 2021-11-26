// FIR_IDENTICAL
abstract class Checker<StateT>

class ToolchainPanel {
    fun <ItemT> addVersionChecker(item: ItemT) {
        class MyState(val selectedItem: ItemT?)
        object : Checker<MyState>() {}
    }
}
