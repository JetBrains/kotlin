// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

val objects = ArrayList<Any>()

fun log(o: Any) {
    objects.add(o)
}

value class Successful(val x: Int = 1.also(::log), val y: Int = 2.also(::log)) {
    init {
        log(3)
    }
    constructor(x: Long = 4L.also(::log), y: Long = 5L.also(::log)): this(x.also(::log).toInt(), y.also(::log).toInt()) {
        log(6)
    }
    init {
        log(7)
    }
    constructor() : this(8.also(::log)) {
        log(9)
    }
}

inline fun errorLogging(x: Any): Nothing = try {
    error(x)
} finally {
    log(x)
}

value class FailingPrimary(val x: Int = errorLogging(10))
value class FailingInit(val x: Int) {
    init {
        errorLogging(11)
    }
}

value class FailingSecondary1(val x: Int) {
    constructor(x: Long = errorLogging(12)) : this(x.toInt())
}

value class FailingSecondary2(val x: Int) {
    constructor(x: Long) : this(errorLogging(13))
}

value class Delegation(val x: Int = 14.also(::log)) : Comparable<Int> by (x.also { log(it + 1) })

inline fun <T> testWithLog(vararg expectedLog: Any?, action: () -> T): Result<T> {
    objects.clear()
    val result = runCatching { action() }
    require(objects == expectedLog.asList()) { "\nExpected:\n${expectedLog.asList().joinToString()}\nGot:\n${objects.joinToString()}" }
    return result
}

fun box(): String {
    val successful1 = testWithLog(8, 2, 3, 7, 9) { Successful() }.getOrThrow()
    require(successful1.toString() == "Successful(x=8, y=2)") { successful1.toString() }
    
    val successful2 = testWithLog(2, 3, 7) { Successful(x = 100) }.getOrThrow()
    require(successful2.toString() == "Successful(x=100, y=2)") { successful2.toString() }
    
    val successful3 = testWithLog(1, 3, 7) { Successful(y = 100) }.getOrThrow()
    require(successful3.toString() == "Successful(x=1, y=100)") { successful3.toString() }
    
    val successful4 = testWithLog(3, 7) { Successful(x = 100, y = 200) }.getOrThrow()
    require(successful4.toString() == "Successful(x=100, y=200)") { successful4.toString() }
    
    val successful5 = testWithLog(3, 7) { Successful(y = 200, x = 100) }.getOrThrow()
    require(successful5.toString() == "Successful(x=100, y=200)") { successful5.toString() }
    
    val successful6 = testWithLog(5L, 100L, 5L, 3, 7, 6) { Successful(x = 100L) }.getOrThrow()
    require(successful6.toString() == "Successful(x=100, y=5)") { successful6.toString() }
    
    val successful7 = testWithLog(4L, 4L, 100L, 3, 7, 6) { Successful(y = 100L) }.getOrThrow()
    require(successful7.toString() == "Successful(x=4, y=100)") { successful7.toString() }
    
    val successful8 = testWithLog(100L, 200L, 3, 7, 6) { Successful(x = 100L, y = 200L) }.getOrThrow()
    require(successful8.toString() == "Successful(x=100, y=200)") { successful8.toString() }
    
    val successful9 = testWithLog(100L, 200L, 3, 7, 6) { Successful(y = 200L, x = 100L) }.getOrThrow()
    require(successful9.toString() == "Successful(x=100, y=200)") { successful9.toString() }
    
    
    val failure1 = testWithLog(10) { FailingPrimary() }.exceptionOrNull()
    require(failure1?.message == "10") { failure1?.message.toString() }
    
    val notFailure1 = testWithLog { FailingPrimary(10) }.getOrThrow()
    require(notFailure1.toString() == "FailingPrimary(x=10)") { notFailure1.toString() }


    val failure2 = testWithLog(11) { FailingInit(100) }.exceptionOrNull()
    require(failure2?.message == "11") { failure2?.message.toString() }
    

    val failure3 = testWithLog(12) { FailingSecondary1() }.exceptionOrNull()
    require(failure3?.message == "12") { failure3?.message.toString() }

    val notFailure3 = testWithLog { FailingSecondary1(20) }.getOrThrow()
    require(notFailure3.toString() == "FailingSecondary1(x=20)") { notFailure3.toString() }

    
    val failure4 = testWithLog(13) { FailingSecondary2(30L) }.exceptionOrNull()
    require(failure4?.message == "13") { failure4?.message.toString() }

    val notFailure4 = testWithLog { FailingSecondary2(30) }.getOrThrow()
    require(notFailure4.toString() == "FailingSecondary2(x=30)") { notFailure4.toString() }

    
    val delegation = testWithLog(14, 15) { Delegation() }.getOrThrow()
    require(delegation.toString() == "Delegation(x=14)") { delegation.toString() }
    
    require(delegation.compareTo(13) > 0)
    require(delegation.compareTo(14) == 0)
    require(delegation.compareTo(15) < 0)
    
    return "OK"
}
