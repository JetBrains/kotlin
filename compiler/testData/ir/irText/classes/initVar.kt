// FIR_IDENTICAL
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
