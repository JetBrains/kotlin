// LANGUAGE: +ContextReceivers
//TARGET_BACKEND: JVM_IR

interface Context
interface Receiver
interface Param

fun foo(context: Context, receiver: Receiver, p: Param) {}

context(Context)
fun bar(receiver: Receiver, p: Param) {}

context(Context)
fun Receiver.baz(p: Param) {}


fun box(): String {
    var a: context(Context, Receiver, Param) () -> Unit // 3, 0, 0
    a = ::foo            
    a = ::bar         
    a = Receiver::baz 

    var b: context(Context, Receiver) Param.() -> Unit // 2, 1, 0
    b = ::foo            
    b = ::bar         
    b = Receiver::baz 

    var c: context(Context, Receiver) (Param) -> Unit // 2, 0, 1
    c = ::foo            
    c = ::bar         
    c = Receiver::baz 

    var d: context(Context) Receiver.(Param) -> Unit // 1, 1, 1. This is the same as in KEEP.
    d = ::foo            
    d = ::bar         
    d = Receiver::baz 

    var e: context(Context) (Receiver, Param) -> Unit // 1, 0, 2
    e = ::foo            
    e = ::bar         
    e = Receiver::baz 

    var f: Context.(Receiver, Param) -> Unit // 0, 1, 2
    f = ::foo            
    f = ::bar         
    f = Receiver::baz 

    var g: (Context, Receiver, Param) -> Unit // 0, 0, 3
    g = ::foo            
    g = ::bar         
    g = Receiver::baz

    return "OK"
}

