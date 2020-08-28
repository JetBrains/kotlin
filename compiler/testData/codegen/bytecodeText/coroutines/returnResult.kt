@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
suspend fun signInFlowStepFirst(): Result<Unit> = Result.success(Unit)

inline class OurAny(val a: Any)

suspend fun returnsUnboxed(): OurAny = OurAny("OK")

// 1 INVOKESTATIC kotlin/Result.box-impl
// 0 INVOKESTATIC kotlin/OurAny.box-impl