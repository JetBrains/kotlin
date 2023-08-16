class A {
    inline fun inlineFun(s: () -> Unit) {
        s()
    }

    fun foo() {
        var s = 1;
        inlineFun ({
                       var zzz = 2;
                       zzz++
                   })
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=zzz\2 TYPE=I INDEX=5
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\2\0 TYPE=I INDEX=4
// VARIABLE : NAME=this_\1 TYPE=LA; INDEX=2
// VARIABLE : NAME=$i$f$inlineFun\1 TYPE=I INDEX=3
// VARIABLE : NAME=s TYPE=I INDEX=1
// VARIABLE : NAME=this TYPE=LA; INDEX=0
