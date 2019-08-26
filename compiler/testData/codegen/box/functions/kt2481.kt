// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box() =
    B().method()

public open class A(){
    public open fun method() : String  = "OK"
}

public class B(): A(){
    public override fun method() : String {
        return ({
          super.method()
        })()
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
