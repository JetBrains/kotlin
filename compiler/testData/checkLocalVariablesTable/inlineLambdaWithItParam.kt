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
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I
// VARIABLE : NAME=$i$f$inlineFun TYPE=I
// VARIABLE : NAME=it TYPE=I
// VARIABLE : NAME=this TYPE=LA;
// VARIABLE : NAME=this_$iv TYPE=LA;
// VARIABLE : NAME=zzz TYPE=I
