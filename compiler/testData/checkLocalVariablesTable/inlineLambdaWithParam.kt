class A {
    inline fun inlineFun(s: (s: Int) -> Unit, p : Int) {
        s(11)

        s(p)
    }

    fun foo() {
        inlineFun ({ l ->
                       var zzz = l;
                       zzz++
                   }, 11)
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=zzz TYPE=I INDEX=6
// VARIABLE : NAME=l TYPE=I INDEX=4
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I INDEX=5
// VARIABLE : NAME=zzz TYPE=I INDEX=6
// VARIABLE : NAME=l TYPE=I INDEX=4
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I INDEX=5
// VARIABLE : NAME=this_$iv TYPE=LA; INDEX=1
// VARIABLE : NAME=p$iv TYPE=I INDEX=2
// VARIABLE : NAME=$i$f$inlineFun TYPE=I INDEX=3
// VARIABLE : NAME=this TYPE=LA; INDEX=0
