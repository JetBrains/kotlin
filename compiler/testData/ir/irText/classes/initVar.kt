class TestInitVarFromParameter(var x: Int)

class TestInitVarInClass {
    var x = 0
}

class TestInitVarInInitBlock {
    var x: Int
    init {
        x = 0
    }
}

class TestInitVarWithCustomSetter {
    var x = 0
        set(value) { field = value }
}

class TestInitVarWithCustomSetterWithExplicitCtor {
    var x: Int
        set(value) { field = value }

    init {
        x = 0
    }

    constructor()
}

class TestInitVarWithCustomSetterInCtor {
    var x: Int set(value) {
        field = value
    }

    constructor() {
        x = 42
    }
}