// FIR_IDENTICAL
package test

interface X {
    fun foo(): String? {
        return null
    }
}

interface Y {
    fun foo(): String {
        return "foo"
    }
}

interface Incompatible {
    fun foo(): Int {
        return 3
    }
}

class Test1(val x: X) : X by x, Y {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> {
        return null
    }
}

class Test2(val x: X) : X by x, Y {
    override fun foo(): String {
        return "foo"
    }
}

class Test3(val y: Y) : X, Y by y {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> {
        return null
    }
}

class Test4(val y: Y) : X, Y by y {
    override fun foo(): String {
        return "foo"
    }
}

class Test5(val y: Y, val x: X) : X by x, Y by y, Incompatible {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Int<!> {
        return 3
    }
}