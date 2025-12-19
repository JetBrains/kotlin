class A {
    inline fun inlineFun(s: () -> Unit) {
        s()
    }

    fun foo() {
        var s = 0;
        inlineFun {
            var z = 1;
            z++

            inlineFun {
                var zz2 = 2;
                zz2++
            }
        }
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1 TYPE=I
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1$1 TYPE=I
// VARIABLE : NAME=$i$f$inlineFun TYPE=I
// VARIABLE : NAME=$i$f$inlineFun TYPE=I
// VARIABLE : NAME=s TYPE=I
// VARIABLE : NAME=this TYPE=LA;
// VARIABLE : NAME=this_$iv TYPE=LA;
// VARIABLE : NAME=this_$iv TYPE=LA;
// VARIABLE : NAME=z TYPE=I
// VARIABLE : NAME=zz2 TYPE=I
