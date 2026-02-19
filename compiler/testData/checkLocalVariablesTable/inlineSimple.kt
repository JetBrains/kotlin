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
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I
// VARIABLE : NAME=$i$f$inlineFun TYPE=I
// VARIABLE : NAME=s TYPE=I
// VARIABLE : NAME=this TYPE=LA;
// VARIABLE : NAME=this_$iv TYPE=LA;
// VARIABLE : NAME=zzz TYPE=I
