// "Change 'bar' function return type to 'Module'" "true"
fun bar(): Any = kotlin.modules.ModuleBuilder("", "")
fun foo(): kotlin.modules.Module = bar(<caret>)