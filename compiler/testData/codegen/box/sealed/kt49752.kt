abstract class Interpreter<IS, TS, SELF>
        where IS : Interpreter.Intermediary<SELF>,
              TS : Interpreter.Terminal<SELF>,
              SELF : Interpreter<IS, TS, SELF> {
    sealed interface Step<INTERPRETER>
            where INTERPRETER : Interpreter<out Intermediary<INTERPRETER>, out Terminal<INTERPRETER>, INTERPRETER>
    interface Intermediary<INTERPRETER> : Step<INTERPRETER>
            where INTERPRETER : Interpreter<out Intermediary<INTERPRETER>, out Terminal<INTERPRETER>, INTERPRETER>
    interface Terminal<INTERPRETER> : Step<INTERPRETER>
            where INTERPRETER : Interpreter<out Intermediary<INTERPRETER>, out Terminal<INTERPRETER>, INTERPRETER>
    abstract fun next(currentStep: IS): () -> Step<SELF>
}

sealed interface BaseTerminal<I : Interpreter<*, out BaseTerminal<I>, I>> : Interpreter.Terminal<I> {
    data class Success<I : Interpreter<*, out BaseTerminal<I>, I>>(
        val result: Int
    ) : BaseTerminal<I>
}

class CountingInterpreter : Interpreter<CountingInterpreter.Intermediary, BaseTerminal.Success<CountingInterpreter>, CountingInterpreter>() {
    sealed interface Intermediary : Interpreter.Intermediary<CountingInterpreter> {
        data class KeepCounting(
            val togo: Int
        ) : Intermediary
    }
    var count = 0
    override fun next(
        currentStep: Intermediary
    ): () -> Step<CountingInterpreter> = {
        when (currentStep) {
            is Intermediary.KeepCounting -> if (currentStep.togo == 0) {
                BaseTerminal.Success(count)
            } else {
                count++
                Intermediary.KeepCounting(currentStep.togo - 1)
            }
        }
    }
}

fun box(): String {
    val interpreter = CountingInterpreter()
    var step: Interpreter.Step<CountingInterpreter> = CountingInterpreter.Intermediary.KeepCounting(1)
    while (step is CountingInterpreter.Intermediary) {
        step = interpreter.next(step).invoke()
    }
    return if ((step as BaseTerminal.Success<CountingInterpreter>).result == 1) "OK"  else "NOK"
}
