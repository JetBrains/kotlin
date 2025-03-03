public class TypeModifiers {
    val function: () -> Unit = null!!

    val suspendFunction: suspend () -> Unit = null!!

    val suspendExtFunction: suspend Any.() -> Unit = null!!

    val functionOnSuspendFunction: (suspend () -> Unit).() -> Unit = null!!
}