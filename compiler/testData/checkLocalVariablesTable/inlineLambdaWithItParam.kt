class A {
    inline fun inlineFun(s: (s: Int) -> Unit) {
        s(11)
    }

    fun foo() {
        inlineFun ({
                       var zzz = it;
                       zzz++
                   })
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=zzz\2 TYPE=I INDEX=5
// VARIABLE : NAME=it\2 TYPE=I INDEX=3
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\2\0 TYPE=I INDEX=4
// VARIABLE : NAME=this_\1 TYPE=LA; INDEX=1
// VARIABLE : NAME=$i$f$inlineFun\1 TYPE=I INDEX=2
// VARIABLE : NAME=this TYPE=LA; INDEX=0
