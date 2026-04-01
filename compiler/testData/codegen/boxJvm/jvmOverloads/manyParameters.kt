// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: JavaCall.java

public class JavaCall {
    public static void test() {
        new Example();
        new Example("1");
    }
}

// FILE: Example.kt

data class Example @JvmOverloads constructor(
    var one: String = "",
    var two: String = "",
    var three: String = "",
    var four: String = "",
    var five: String = "",
    var six: String = "",
    var seven: String = "",
    var eight: String = "",
    var nine: String = "",
    var ten: String = "",
    var eleven: String = "",
    var twelve: String = "",
    var thirteen: String = "",
    var fourteen: String = "",
    var fifteen: String = "",
    var sixteen: String = "",
    var seventeen: String = "",
    var eighteen: String = "",
    var nineteen: String = "",
    var twenty: String = "",
    var twentyOne: String = "",
    var twentyTwo: String = "",
    var twentyThree: String = "",
    var twentyFour: String = "",
    var twentyFive: String = "",
    var twentySix: String = "",
    var twentySeven: String = "",
    var twentyEight: String = "",
    var twentyNine: String = "",
    var thirty: String = "",
    var thirtyOne: String = "",
    var thirtyTwo: String = "",
    var thirtyThree: String = "",
    var thirtyFour: String = ""
)

fun box(): String {
    Example()
    JavaCall.test()
    return "OK"
}
