class My(val x: My?, val z: My? = null)

fun baseTest() {
    var y: My? = My(My(null))
    if (y?.x != null) {
        y.x.x
        y = My(null)
        y.x<!UNSAFE_CALL!>.<!>x
    }
}

fun deepChainTest() {
    var y: My? = My(My(null))
    if (y?.x?.x != null) {
        y.x.x.x
        y = My(null)
        y.x<!UNSAFE_CALL!>.<!>x<!UNSAFE_CALL!>.<!>x
    }
}

fun backwardAliasTest(z: My) {
    var y = z
    if (y.x != null) {
        y.x.x
        y = My(null)
        y.x<!UNSAFE_CALL!>.<!>x
    }
}

fun severalMembersTest() {
    var y = My(My(null), My(null))
    if (y.x != null) {
        if (y.z != null) {
            y.x.x
            y.z.z
            y = My(null)
            y.x<!UNSAFE_CALL!>.<!>x
            y.z<!UNSAFE_CALL!>.<!>z
        }
    }
}