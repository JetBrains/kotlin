// FIR_IDENTICAL
// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference

fun main() {
    Configuration().commands {
        Command1 { someService::execute } // Overload resolution ambiguity. All these functions match.
        Command2 { someService::execute } // Overload resolution ambiguity. All these functions match.
        Command1 { { someService.execute(it) } } // fine
        Command2 { { someService.execute(it) } } // fine
    }
}
interface Command
interface CommandFactory<TCommand : Command>
class Command1 : Command {
    companion object : CommandFactory<Command1>
}
class Command2 : Command {
    companion object : CommandFactory<Command2>
}
class Configuration {
    val commands = Commands()
    inline fun commands(configure: Commands.() -> Unit) {
        commands.configure()
    }
    class Commands {
        operator fun <TCommand : Command> CommandFactory<TCommand>.invoke(
            handler: Transaction.() -> ((command: TCommand) -> Unit)
        ) {
        }
    }
}
interface Transaction {
    val someService: SomeService
}
interface SomeService {
    fun execute(command: Command1)
    fun execute(command: Command2)
}
