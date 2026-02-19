// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FULL_JDK

import java.util.*
import java.util.stream.Stream

fun <T> Stream<T>?.getIfSingle() =
        this?.map { Optional.ofNullable(it) }
        ?.reduce(Optional.empty()) { _, _ -> Optional.empty() }
        ?.orElse(null) // <<---- should not be an error

/* GENERATED_FIR_TAGS: dnnType, flexibleType, funWithExtensionReceiver, functionDeclaration, inProjection, javaFunction,
lambdaLiteral, nullableType, outProjection, safeCall, samConversion, thisExpression, typeParameter */
