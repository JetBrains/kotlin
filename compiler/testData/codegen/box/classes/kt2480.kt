// IGNORE_BACKEND_FIR: JVM_IR
fun box() = Class().printSome()

public abstract class AbstractClass<T> {
    public fun printSome() : T = some

    public abstract val some: T
}

public class Class: AbstractClass<String>() {
    public override val some: String
        get() = "OK"

}
