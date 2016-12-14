public class TypeModifiers {
    val suspendFunction: suspend () -> Unit = null!!

    val suspendExtFunction: suspend Any.() -> Unit = null!!

    val functionOnSuspendFunction: (suspend () -> Unit).() -> Unit = null!!
}