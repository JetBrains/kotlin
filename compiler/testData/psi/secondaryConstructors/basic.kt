class A {
    constructor() {}

    val x: Int

    private annot constructor(x: Int) {}

    [constructor] fun constructor() {}

    annot protected constructor(x: Int, y: Int) : this(1,2) {}

    [constructor] public constructor() : super() {
        x = 1
    }
}

constructor class B
