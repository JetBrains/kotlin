class A
class B

typealias WithGeneric<T> = (T) -> String
fun withGeneric(f: WithGeneric<Double>) {}

typealias WithReceiver = String.() -> Unit
fun withReceiver(f: WithReceiver) {}

typealias WithContextReceiver = context(String) () -> Unit
fun withContextReceiver(f: WithContextReceiver) {}

typealias WithSuspend = suspend (String) -> Int
fun withContextReceiver(f: WithSuspend) {}

typealias WithEverything<T> = suspend context(T, B) String.(Int, T) -> String
fun withEverything(f: WithEverything<A>) {}