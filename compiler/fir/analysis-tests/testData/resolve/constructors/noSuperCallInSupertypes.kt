// FILE: AJava.java

public class AJava {
    public AJava(String s) {}
}

// FILE: main.kt

class BJava : AJava, C {
    constructor(s: String) : super(s)
}

open class AKt(val s: String)

class BKt : AKt, C {
    constructor(s: String) : super(s)
}

interface C

typealias QQQ = AKt

typealias DDD = C

class CKt : QQQ, DDD {
    constructor(s: String) : super(s)
}
