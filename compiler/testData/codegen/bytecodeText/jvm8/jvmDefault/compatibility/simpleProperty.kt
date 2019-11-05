// !JVM_DEFAULT_MODE: compatibility
// JVM_TARGET: 1.8

interface KInterface {

    @JvmDefault
    var bar: String
        get() = "OK"
        set(field) {}
}

interface KInterface2 : KInterface {

}

// 1 INVOKESTATIC KInterface2.access\$getBar\$
// 1 INVOKESTATIC KInterface2.access\$setBar\$
// 1 INVOKESTATIC KInterface.access\$getBar\$
// 1 INVOKESTATIC KInterface.access\$setBar\$

// 1 INVOKESPECIAL KInterface2.getBar
// 1 INVOKESPECIAL KInterface2.setBar
// 1 INVOKESPECIAL KInterface.getBar
// 1 INVOKESPECIAL KInterface.setBar
