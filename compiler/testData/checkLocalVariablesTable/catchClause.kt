class A {
    fun foo() {
        try {
            var a = 1
            a++
        }
        catch(e : Throwable) {
            e.printStackTrace()
        }
    }
}

// METHOD : A.foo()V
// VARIABLE : NAME=a TYPE=I INDEX=1
// VARIABLE : NAME=e TYPE=Ljava/lang/Throwable; INDEX=1
// VARIABLE : NAME=this TYPE=LA; INDEX=0
