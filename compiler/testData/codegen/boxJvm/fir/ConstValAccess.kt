// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: A.kt

object Obj {
    const val A_CONST = "O"
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val s = B_CONST
    return s + "K";
}

const val B_CONST = Obj.A_CONST
