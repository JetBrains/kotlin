class C {
    val property1: Int = 0
    var property2: Int = 0
    var xxx: Int = 0

    fun setPPP(){}
}

fun foo(c: C) {
    c.setP<caret>
}

// ABSENT: property1
// EXIST: property2
// EXIST: setPPP
// ABSENT: xxx
