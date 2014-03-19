class A {
    inline fun inlineFun(s: () -> Unit) {
        s()
    }

    fun foo() {
        var s = 0;
        inlineFun {
            var z = 1;

            inlineFun {
                var zz2 = 2;
            }
        }
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=zz2 TYPE=I INDEX=5
// VARIABLE : NAME=z TYPE=I INDEX=3
// VARIABLE : NAME=s TYPE=I INDEX=1
// VARIABLE : NAME=this TYPE=LA; INDEX=0
