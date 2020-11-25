class A {
    companion object {
        val s: String
            get() = "Ok"
        var v : String
            get() = "NOT OK"
            set(value) {}
    }

    inline fun f(): String = s

    inline fun g() {
        v = "OK"
    }
}

// No backing field on A and all accesses call the getter/setter.
// 0 GETSTATIC A.s
// 0 PUTSTATIC A.v

// One `getS` call in `f` and one `setV` call in `g`
// 1 INVOKEVIRTUAL A\$Companion.getS
// 1 INVOKEVIRTUAL A\$Companion.setV