// WITH_STDLIB
// NO_OPTIMIZED_CALLABLE_REFERENCES

class A {
    val x = "OK"
    val y by ::x
}

fun box(): String = A().y
