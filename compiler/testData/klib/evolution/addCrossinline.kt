// KT-44599: expected:<[OK]> but was:<[UNEXPECTED RETURN]>
// IGNORE_BACKEND: JS_IR
// KT-44599: exception: org.jetbrains.kotlin.backend.konan.llvm.NativeCodeGeneratorException: Exception during generating code for following declaration:
// IGNORE_BACKEND: NATIVE

// MODULE: lib
// FILE: A.kt
// VERSION: 1

inline fun foo(x: () -> Unit) = x()

// FILE: B.kt
// VERSION: 2

fun regular(y: () -> Unit) = y()
inline fun foo(crossinline x: () -> Unit) = regular{ x() }

// MODULE: mainLib(lib)
// FILE: mainLib.kt

val list = mutableListOf('p', 'a', 'j', 'a', 'm', 'a', 's')

fun lib(): String {
    foo {
        list.reverse()
        return "OK"
    }

    return "UNEXPECTED RETURN"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()
