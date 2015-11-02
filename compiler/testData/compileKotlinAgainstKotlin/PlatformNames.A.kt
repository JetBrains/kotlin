package lib

@JvmName("bar")
fun foo() = "foo"

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set
