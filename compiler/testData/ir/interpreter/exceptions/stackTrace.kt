@CompileTimeCalculation
fun divide(a: Int, b: Int) = a / b

@CompileTimeCalculation
fun echo(a : Int, b: Int) = divide(a, b)

@CompileTimeCalculation
fun getLengthOrThrowException(str: String?): Int {
    try {
        return str!!.length
    } catch (e: NullPointerException) {
        throw UnsupportedOperationException(e)
    }
}

const val a = <!WAS_NOT_EVALUATED: `
Exception java.lang.ArithmeticException: / by zero
	at StackTraceKt.divide(stackTrace.kt:2)
	at StackTraceKt.echo(stackTrace.kt:5)
	at StackTraceKt.<clinit>(stackTrace.kt:16)`!>echo(1, 0)<!>
const val b = <!WAS_NOT_EVALUATED: `
Exception java.lang.UnsupportedOperationException: java.lang.NullPointerException
	at StackTraceKt.getLengthOrThrowException(stackTrace.kt:12)
	at StackTraceKt.<clinit>(stackTrace.kt:17)
Caused by: java.lang.NullPointerException
	at StackTraceKt.getLengthOrThrowException(stackTrace.kt:10)
	at StackTraceKt.<clinit>(stackTrace.kt:17)`!>getLengthOrThrowException(null)<!>
