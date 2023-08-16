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
// VARIABLE : NAME=zz2\4 TYPE=I INDEX=9
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1$1\4\2 TYPE=I INDEX=8
// VARIABLE : NAME=this_\3 TYPE=LA; INDEX=6
// VARIABLE : NAME=$i$f$inlineFun\3 TYPE=I INDEX=7
// VARIABLE : NAME=z\2 TYPE=I INDEX=5
// VARIABLE : NAME=$i$a$-inlineFun-A$foo$1\2\0 TYPE=I INDEX=4
// VARIABLE : NAME=this_\1 TYPE=LA; INDEX=2
// VARIABLE : NAME=$i$f$inlineFun\1 TYPE=I INDEX=3
// VARIABLE : NAME=s TYPE=I INDEX=1
// VARIABLE : NAME=this TYPE=LA; INDEX=0
