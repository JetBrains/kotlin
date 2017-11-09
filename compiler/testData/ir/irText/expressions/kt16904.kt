// FILE: J.java

public class J {
    public int field = 0;
}

// FILE: kt16904.kt

abstract class A {
    val x = B()
    var y = 0
}

class B {
    operator fun plusAssign(x: Int) {
    }
}

class Test1 : A {
    constructor() {
        x += 42
        y += 42
    }
}

class Test2 : J() {
    init {
        field = 42
    }
}