@CompileTimeCalculation
fun infinityWhile(): Int {
    while (true) {}
    return 0
}

const val a = <!WAS_NOT_EVALUATED: `
Exception org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterTimeOutError: Exceeded execution limit of constexpr expression
	at CommandsOutExceptionKt.infinityWhile(commandsOutException.kt:3)
	at CommandsOutExceptionKt.<clinit>(commandsOutException.kt:7)`!>infinityWhile()<!>
