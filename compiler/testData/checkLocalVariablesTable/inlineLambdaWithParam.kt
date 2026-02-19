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
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I
// VARIABLE : NAME=$i$f$inlineFun TYPE=I
// VARIABLE : NAME=l TYPE=I
// VARIABLE : NAME=l TYPE=I
// VARIABLE : NAME=p$iv TYPE=I
// VARIABLE : NAME=this TYPE=LA;
// VARIABLE : NAME=this_$iv TYPE=LA;
// VARIABLE : NAME=zzz TYPE=I
// VARIABLE : NAME=zzz TYPE=I
