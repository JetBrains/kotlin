// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

inline fun foo(bar1: (String.() -> Int) -> Int, bar2: (()->Int) -> Int) {
    bar1 label@ {
        this@label.length
    }

    bar1 {
        this.length
    }
    //unmute after KT-4247 fix
    //bar1  {
    //    this@bar1.length
    //}

    bar2 l@ {
        11
    }

    bar2 {
        12
    }

}

inline fun foo2(bar1: (String.() -> Int) -> Int) {
    l1@ <!USAGE_IS_NOT_INLINABLE!>bar1<!>

    l2@ bar1 {
        11
    }

    (l3@ bar1) {
        11
    }

    (l5@ (l4@ bar1)) {
        11
    }
}
