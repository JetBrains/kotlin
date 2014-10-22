// "Change getter type to Module" "true"
class A() {
    val i: kotlin.modules.Module
        get(): <caret>Any = kotlin.modules.ModuleBuilder("", "")
}
