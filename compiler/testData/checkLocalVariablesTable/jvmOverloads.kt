// IGNORE_BACKEND: JVM_IR
class C {
    @kotlin.jvm.JvmOverloads fun foo(firstParam: Int, secondParam: String = "") {
    }
}

// METHOD : C.foo(I)V
// VARIABLE : NAME=firstParam TYPE=I INDEX=1
