
import test.referenceToJavaFieldViaBridge

class A : referenceToJavaFieldViaBridge() {

    fun a(): String {
        return {field!!}()
    }
}

fun box(): String {
    return A().a()
}