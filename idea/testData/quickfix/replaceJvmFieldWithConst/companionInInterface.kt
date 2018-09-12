// "Replace '@JvmField' with 'const'" "true"
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2
interface IFace {
    companion object {
        <caret>@JvmField val a = "Lorem ipsum"
    }
}