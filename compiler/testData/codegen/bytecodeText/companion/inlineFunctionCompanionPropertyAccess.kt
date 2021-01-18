class A {
    companion object {
        val s = "OK"
        var v = "NOT OK"
    }

    inline fun f(): String = s

    inline fun g() {
        v = "OK"
    }
}

// One direct `A.s` access in the accessibility bridge `access$getS$cp`.
// 1 GETSTATIC A.s

// One direct `A.v` set in the accessibility bridge `access$setV$cp`.
// One direct `A.v` set in `A.<clinit>`
// 2 PUTSTATIC A.v

// One call to the getter/setter on the companion object from f and one from g.
// 1 INVOKEVIRTUAL A\$Companion.getS
// 1 INVOKEVIRTUAL A\$Companion.setV

// One call to the accessibility bridge `access$setV$cp` from Companion.setV.
// 1 INVOKESTATIC A.access\$setV\$cp

// One call to the accessibility bridge `access$getS$cp` from Companion.getS.
// 1 INVOKESTATIC A.access\$getS\$cp
