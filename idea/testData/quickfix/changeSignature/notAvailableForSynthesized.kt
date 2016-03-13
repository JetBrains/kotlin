// "class org.jetbrains.kotlin.idea.quickfix.AddFunctionParametersFix" "false"
//ERROR: Too many arguments for public final operator fun component1(): Int defined in Data

data class Data(val i: Int) {}

fun usage(d: Data) {
    d.component1(<caret>2)
}
