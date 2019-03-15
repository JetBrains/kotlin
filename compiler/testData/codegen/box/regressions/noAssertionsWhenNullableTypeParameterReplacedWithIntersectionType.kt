// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR

class Recursive<T : Recursive<T>> : Generic<PlaceHolder<T>>, MainSupertype
open class Simple<T> : Generic<T>, MainSupertype

interface Generic<T>

interface PlaceHolder<T : MainSupertype> : Stub<T>
interface Stub<T : MainSupertype>

interface MainSupertype

interface SpecificStub<T : SpecificSimple> : Stub<T>
abstract class SpecificSimple : Simple<SpecificStub<*>>()


fun takeElement(recursive: Recursive<*>?, simpleWithSpecific: Simple<SpecificStub<*>>) {
    select(recursive, simpleWithSpecific) // T -> intersection type
}

fun <T> select(x: T?, y: T): T = y

fun box(): String {
    takeElement(null, Simple())
    return "OK"
}