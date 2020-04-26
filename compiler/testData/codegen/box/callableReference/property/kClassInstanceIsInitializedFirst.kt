import kotlin.reflect.KProperty1

class A {
    companion object {
        val ref: KProperty1<A, String> = A::foo
    }

    val foo: String = "OK"
}

fun box(): String {
    return A.ref.get(A())
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
