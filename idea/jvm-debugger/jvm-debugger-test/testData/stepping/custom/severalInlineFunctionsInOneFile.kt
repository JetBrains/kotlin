// FILE: severalInlineCallsFromOtherFile.kt
package severalInlineCallsFromOtherFile

fun main(args: Array<String>) {
    //Breakpoint!
    inlineCalls()           // line 5
}

fun inlineCalls() {
    one()                   // line 23
    ObjectOne.one()         // line 29
    ClassOne().one()        // line 41

    two()                   // line 66
    ObjectOne.two()         // line 34
    ClassOne().two()        // line 46

    ObjectTwo.one()         // line 53
    ClassTwo().one()        // line 60
}

inline fun one() {
    //Breakpoint!
    some()
}

object ObjectOne {
    inline fun one() {
        //Breakpoint!
        some()
    }

    inline fun two() {
        //Breakpoint!
        some()
    }
}

class ClassOne {
    inline fun one() {
        //Breakpoint!
        some()
    }

    inline fun two() {
        //Breakpoint!
        some()
    }
}

object ObjectTwo {
    inline fun one() {
        //Breakpoint!
        some()
    }
}

class ClassTwo {
    inline fun one() {
        //Breakpoint!
        some()
    }
}

inline fun two() {
    //Breakpoint!
    some()
}

inline fun some() = 1

// RESUME: 9