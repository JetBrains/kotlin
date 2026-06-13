// LANGUAGE: +FullValueClasses
// WITH_STDLIB

val log = mutableListOf<Any>()

abstract value class Base {
    abstract val i: Int

    init {
        log.add("Base.init:i=$i")
    }
}

value class Derived(override val i: Int, val f: Float) : Base() {
    init {
        log.add("Derived.init:i=$i,f=$f")
    }
}

fun box(): String {
    log.clear()
    val d = Derived(42, 3.14f)
    require(d.i == 42) { "d.i=${d.i}" }
    require(d.f == 3.14f) { "d.f=${d.f}" }

    val expected = listOf("Base.init:i=42", "Derived.init:i=42,f=3.14")
    require(log == expected) { "Expected:\n$expected\nGot:\n$log" }

    return "OK"
}
