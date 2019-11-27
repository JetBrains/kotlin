// IGNORE_BACKEND_FIR: JVM_IR
open class LockFreeLinkedListNode(val s: String)
private class SendBuffered(s: String) : LockFreeLinkedListNode(s)
open class AddLastDesc2<out T : LockFreeLinkedListNode>(val node: T)
typealias AddLastDesc<T> = AddLastDesc2<T>

fun describeSendBuffered(): AddLastDesc<*> {
    return object : AddLastDesc<SendBuffered>(SendBuffered("OK")) {}
}

fun box() = describeSendBuffered().node.s
