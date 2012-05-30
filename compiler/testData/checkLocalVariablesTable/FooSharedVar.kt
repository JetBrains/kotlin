class FooSharedVar {
    fun bar(var x: Int) {
        fun bar1() {
            x = x + 2
        }
    }
}

// METHOD : bar(I)V
// VARIABLE : NAME=this TYPE=LFooSharedVar; INDEX=0
// VARIABLE : NAME=x TYPE=I INDEX=1
// VARIABLE : NAME=svx TYPE=Ljet/runtime/SharedVar$Int; INDEX=1