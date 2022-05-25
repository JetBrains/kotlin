// TARGET_BACKEND: NATIVE

// MODULE: clib
// FILE: clib.def
package = clib
strictEnums = E
---
enum E {
    X = 1, Y = 2, Z = 42
};

typedef struct {
    int d;
} Struct;

// MODULE: lib(clib)
// FILE: lib.kt
import clib.*
import kotlinx.cinterop.*

fun bar1(e: E) = e.value

inline fun foo1() = bar1(E.Z)

fun bar2(s: Struct): Int {
    return s.d
}

inline fun foo2(): Int {
    memScoped {
        val s = alloc<Struct>()
        s.d = 42
        return bar2(s)
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String = when {
    foo1() != 42u -> "FAIL 1"
    foo2() != 42 -> "FAIL 2"
    else -> "OK"
}
