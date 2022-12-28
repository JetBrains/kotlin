// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// Issues: KT-25105

class Message1
class Task<T>
object Message2
enum class Message3
data class Message4(val x: Int)

sealed class Message5<T> {
    open fun <A : T> execute() {}
}

interface Manager<T> {
    fun <A : T> execute1(task: Task<A>) {}
    fun <T : <!FINAL_UPPER_BOUND!>Int<!>> execute2(task: T) {}
    fun <T : <!FINAL_UPPER_BOUND!>Message2<!>> execute3() {}
    fun <A : T> execute4() {}
    fun <A : T> execute5() {}
    val <A : T> A.x get() = 10
    var <A : T> A.y
        get() = 10
        set(value) {}
}

object MessageManager1 : Manager<Message1> {
    override fun <T : Message1> execute1(task: Task<T>) {}
    override fun <T : Int> execute2(task: T) {}
    override fun <T : Message2> execute3() {}
}

object MessageManager2 : Manager<Message3> {
    override fun <T : Message3> execute4() {}
}

object MessageManager3 : Manager<Message4> {
    override fun <T : Message4> execute5() {}
}

class MessageManager4 : Manager<Message1> {
    override fun <T : Message1> execute1(task: Task<T>) {}
    override fun <T : Int> execute2(task: T) {}
    override fun <T : Message2> execute3() {}
}

class MessageManager5 : Manager<Message3> {
    override fun <T : Message3> execute4() {}
}

class MessageManager6 : Manager<Message4> {
    override fun <T : Message4> execute5() {}
}

interface MessageManager7 : Manager<Message4> {
    override fun <T : Message4> execute5() {}
}

interface MessageManager8 : Manager<Message1> {
    override fun <T : Message1> execute1(task: Task<T>) {}
    override fun <T : Int> execute2(task: T) {}
    override fun <T : Message2> execute3() {}
}

interface MessageManager9 : Manager<Message3> {
    override fun <T : Message3> execute4() {}
}

object MessageManager10 : Message5<Int>() {
    override fun <T : Int> execute() {}
}

class MessageManager11<A> : Message5<Message5<A>>() {
    override fun <T : Message5<A>> execute() {}
}

data class MessageManager12(val x: Int) : Message5<Message2>() {
    override fun <T : Message2> execute() {}
}

sealed class MessageManager13<A> : Message5<A>() {
    override fun <T : A> execute() {}
}

class MessageManager14 : Manager<Message2> {
    override val <T : Message2> T.x get() = 10
    override var <T : Message2> T.y
        get() = 10
        set(value) {}
}

object MessageManager15 : Manager<Int> {
    override val <T : Int> T.x get() = 10
    override var <T : Int> T.y
        get() = 10
        set(value) {}
}
