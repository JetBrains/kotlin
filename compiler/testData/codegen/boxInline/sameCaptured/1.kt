import test.*

fun testSameCaptured() : String {
    var result = 0;
    result = doWork({result+=1; result}, {result += 11; result})
    return if (result == 12) "OK" else "fail ${result}"
}

inline fun testSameCaptured(lambdaWithResultCaptured: () -> Unit) : String {
    var result = 1;
    result = doWork({result+=11; lambdaWithResultCaptured(); result})
    return if (result == 12) "OK" else "fail ${result}"
}

fun box(): String {
    if (testSameCaptured() != "OK") return "test1 : ${testSameCaptured()}"

    var result = 0;
    if (testSameCaptured{result += 1111} != "OK") return "test2 : ${testSameCaptured{result = 1111}}"

    return "OK"
}