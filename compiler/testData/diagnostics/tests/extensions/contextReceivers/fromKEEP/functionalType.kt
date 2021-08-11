// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

class Context
class Receiver
class Param

fun foo(context: Context, receiver: Receiver, p: Param) {}

context(Context)
fun bar(receiver: Receiver, p: Param) {}

context(Context)
fun Receiver.baz(p: Param) {}

fun main() {
    var g: context(Context) Receiver.(Param) -> Unit
    g = ::foo         // OK
    g = ::bar         // OK
    g = Receiver::baz // OK
}