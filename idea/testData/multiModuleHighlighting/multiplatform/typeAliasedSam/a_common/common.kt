package foo

expect interface Runnble {
    public abstract fun run(): kotlin.Unit
}

public expect inline fun Runnble(crossinline block: () -> kotlin.Unit): Runnble
