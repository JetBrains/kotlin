// MODULE: lib1
// FILE: lib1.kt
fun initializer() = 4

// MODULE: lib2(lib1)
// FILE: lib2.kt
val four = initializer()

// MODULE: main(lib1, lib2)
// FILE: main.kt
fun box(): String {
    if (four == 4)
        return "OK"
    else
        return four.toString()
}
