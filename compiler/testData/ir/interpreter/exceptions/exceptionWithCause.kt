// https://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Throwable.html#printStackTrace()
@CompileTimeCalculation
class HighLevelException(cause: Throwable?) : Exception(cause)
@CompileTimeCalculation
class MidLevelException(cause: Throwable?) : Exception(cause)
@CompileTimeCalculation
class LowLevelException : Exception()

@CompileTimeCalculation
class Junk {
    public fun a(): Nothing {
        try {
            b()
        } catch (e: MidLevelException) {
            throw HighLevelException(e)
        }
    }

    private fun b(): Nothing = c()

    private fun c(): Nothing {
        try {
            d()
        } catch (e: LowLevelException) {
            throw MidLevelException(e)
        }
    }

    private fun d(): Nothing = e()

    private fun e(): Nothing = throw LowLevelException()
}

const val exceptionWithCause = <!WAS_NOT_EVALUATED: `
Exception HighLevelException: MidLevelException: LowLevelException
	at ExceptionWithCauseKt.Junk.a(exceptionWithCause.kt:15)
	at ExceptionWithCauseKt.<clinit>(exceptionWithCause.kt:34)
Caused by: MidLevelException: LowLevelException
	at ExceptionWithCauseKt.Junk.c(exceptionWithCause.kt:25)
	at ExceptionWithCauseKt.Junk.b(exceptionWithCause.kt:19)
	at ExceptionWithCauseKt.Junk.a(exceptionWithCause.kt:13)
	at ExceptionWithCauseKt.<clinit>(exceptionWithCause.kt:34)
Caused by: LowLevelException
	at ExceptionWithCauseKt.Junk.e(exceptionWithCause.kt:31)
	at ExceptionWithCauseKt.Junk.d(exceptionWithCause.kt:29)
	at ExceptionWithCauseKt.Junk.c(exceptionWithCause.kt:23)
	at ExceptionWithCauseKt.Junk.b(exceptionWithCause.kt:19)
	at ExceptionWithCauseKt.Junk.a(exceptionWithCause.kt:13)
	at ExceptionWithCauseKt.<clinit>(exceptionWithCause.kt:34)`!>Junk().a().toString()<!>
