class Outer1 {
    class Nested1

    private @Ann constructor()
}

class Outer2 {
    class Nested2;

    private @Ann constructor()
}

class Outer3 {
    class Nested3

    private @Ann constructor() : super() {}
}

class Outer4 {
    object Nested4
    constructor() {}

    object Nested5 private constructor() : super() {}
}

object TopLevel constructor(val x: Int) {
    fun foo() {}
}
