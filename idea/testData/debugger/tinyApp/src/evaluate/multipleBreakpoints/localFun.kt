package localFun

fun main(args: Array<String>) {
    fun myLocalFun1() = 1

    // EXPRESSION: myLocalFun1()
    // RESULT: 1: I
    //Breakpoint!
    myLocalFun1()

    fun myLocalFun2() = 2
    // EXPRESSION: myLocalFun2()
    // RESULT: 2: I
    //Breakpoint!
    myLocalFun2()

    fun myLocalFun3() {
        // EXPRESSION: myLocalFun1() + 1
        // RESULT: java.lang.AssertionError : Cannot find local variable: name = myLocalFun1
        //Breakpoint!
        myLocalFun1() + 1
    }

    myLocalFun3()


    fun myLocalFun4(): Int {
        return 1
    }

    // EXPRESSION: myLocalFun4()
    // RESULT: 1: I
    //Breakpoint!
    myLocalFun4()

    fun myLocalFun5(i: Int): Int {
        return i
    }

    // EXPRESSION: myLocalFun5(2)
    // RESULT: 2: I
    //Breakpoint!
    myLocalFun5(2)

    var i = 1
    fun myLocalFun6(): Int {
        i++
        return i
    }

    // EXPRESSION: myLocalFun6()
    // RESULT: 2: I
    //Breakpoint!
    myLocalFun6()

    i = 1
    fun myLocalFun7() {
        // EXPRESSION: myLocalFun6() + 1
        // RESULT: java.lang.AssertionError : Cannot find local variable: name = myLocalFun6
        //Breakpoint!
        myLocalFun6() + 1
    }

    myLocalFun7()
}