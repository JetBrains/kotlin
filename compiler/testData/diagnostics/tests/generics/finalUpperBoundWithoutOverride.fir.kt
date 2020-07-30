// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// Issues: KT-25105

class Message1
class Task<T>
object Message2
enum class Message3
data class Message4(val x: Int)
interface Manager<T> {}

object MessageManager1 : Manager<Message1> {
    fun <T : Message1> execute1(task: Task<T>) {}
    fun <T : Int> execute2(task: T) {}
    fun <T : Message2> execute3() {}
}

object MessageManager2 : Manager<Message3> {
    fun <T : Message3> execute4() {}
}

object MessageManager3 : Manager<Message4> {
    fun <T : Message4> execute5() {}
}

class MessageManager4 : Manager<Message1> {
    fun <T : Message1> execute1(task: Task<T>) {}
    fun <T : Int> execute2(task: T) {}
    fun <T : Message2> execute3() {}
}

class MessageManager5 : Manager<Message3> {
    fun <T : Message3> execute4() {}
}

class MessageManager6 : Manager<Message4> {
    fun <T : Message4> execute5() {}
}

interface MessageManager7 : Manager<Message4> {
    fun <T : Message4> execute5() {}
}

interface MessageManager8 : Manager<Message1> {
    fun <T : Message1> execute1(task: Task<T>) {}
    fun <T : Int> execute2(task: T) {}
    fun <T : Message2> execute3() {}
}

interface MessageManager9 : Manager<Message3> {
    fun <T : Message3> execute4() {}
}

object MessageManager10 : <!UNRESOLVED_REFERENCE!>Message5<Int><!>() {
    fun <T : Int> execute() {}
}

class MessageManager11<A> : <!UNRESOLVED_REFERENCE!>Message5<Message5<A>><!>() {
    fun <T : <!UNRESOLVED_REFERENCE!>Message5<A><!>> execute() {}
}

data class MessageManager12(val x: Int) : <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Message5<Message2><!>() {
    fun <T : Message2> execute() {}
}

sealed class MessageManager13<A> : <!UNRESOLVED_REFERENCE!>Message5<A><!>() {
    fun <T : A> execute() {}
}

class MessageManager14 : Manager<Message2> {
    val <T : Message2> T.x get() = 10
    var <T : Message2> T.y
        get() = 10
        set(value) {}
}

object MessageManager15 : Manager<Int> {
    val <T : Int> T.x get() = 10
    var <T : Int> T.y
        get() = 10
        set(value) {}
}
