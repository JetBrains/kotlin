// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class Request(val path: String) {

}

class Handler() {
    fun Int.times(op: ()-> Unit) {
        for(i in 0..this)
            op()
    }

//    fun Request.getPath() : String {
//        val sb = java.lang.StringBuilder()
//        10.times {
//            sb.append(path)?.append(this)
//        }
//        return sb.toString() as String
//    }

    fun Request.getPath() = path

    fun test(request: Request) = request.getPath()
}

fun box() : String = if(Handler().test(Request("239")) == "239") "OK" else "fail"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
