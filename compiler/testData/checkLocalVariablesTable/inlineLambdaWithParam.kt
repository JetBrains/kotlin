class A {
    inline fun inlineFun(s: (s: Int) -> Unit, p : Int) {
        s(11)

        s(p)
    }

    fun foo() {
        inlineFun ({ l ->
                       var zzz = l;
                   }, 11)
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=zzz TYPE=I INDEX=4
// VARIABLE : NAME=l TYPE=I INDEX=3
// VARIABLE : NAME=zzz TYPE=I INDEX=4
// VARIABLE : NAME=l TYPE=I INDEX=3
// VARIABLE : NAME=p TYPE=I INDEX=2
// VARIABLE : NAME=this TYPE=LA; INDEX=0
