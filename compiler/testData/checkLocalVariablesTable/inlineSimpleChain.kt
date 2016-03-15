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
// VARIABLE : NAME=zz2 TYPE=I INDEX=5
// VARIABLE : NAME=$i$a$1$inlineFun TYPE=I INDEX=6
// VARIABLE : NAME=this_$iv TYPE=LA; INDEX=4
// VARIABLE : NAME=$i$f$inlineFun TYPE=I INDEX=7
// VARIABLE : NAME=z TYPE=I INDEX=3
// VARIABLE : NAME=$i$a$1$inlineFun TYPE=I INDEX=8
// VARIABLE : NAME=this_$iv TYPE=LA; INDEX=2
// VARIABLE : NAME=$i$f$inlineFun TYPE=I INDEX=9
// VARIABLE : NAME=s TYPE=I INDEX=1
// VARIABLE : NAME=this TYPE=LA; INDEX=0
