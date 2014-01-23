// "class org.jetbrains.jet.plugin.quickfix.AddFunctionParametersFix" "false"
//ERROR: Too many arguments for internal final fun component1(): jet.Int defined in Data

data class Data(val i: Int) {}

fun usage(d: Data) {
    d.component1(<caret>2)
}