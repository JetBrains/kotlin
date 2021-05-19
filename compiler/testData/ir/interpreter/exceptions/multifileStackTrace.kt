// FILE: main.kt

@CompileTimeCalculation
fun callToOtherFile(mustThrowException: Boolean, message: String): Int {
    if (mustThrowException) throwException(message)
    return 0
}

const val a = <!WAS_NOT_EVALUATED: `
Exception java.lang.Exception: Exception from file1.kt
	at File1Kt.throwException(file1.kt:19)
	at MainKt.callToOtherFile(main.kt:5)
	at MainKt.<clinit>(main.kt:9)`!>callToOtherFile(true, "Exception from file1.kt")<!>

// FILE: file1.kt

@CompileTimeCalculation
fun throwException(message: String) {
    throw Exception(message)
}
