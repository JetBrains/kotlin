// LANGUAGE: +NameBasedDestructuring
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-81555
// simple.kts
var result = "getter must be called"

class C {
    val myProp: String
        get() {
            result = "OK"
            return ""
        }
}

(val _ = myProp) = C()

// expected: result: OK