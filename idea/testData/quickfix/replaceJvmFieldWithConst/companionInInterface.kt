// "Replace '@JvmField' with 'const'" "true"
// WITH_RUNTIME
interface IFace {
    companion object {
        <caret>@JvmField val a = "Lorem ipsum"
    }
}