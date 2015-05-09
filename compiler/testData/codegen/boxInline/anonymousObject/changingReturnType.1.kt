//NO_CHECK_LAMBDA_INLINING
//KT-7490
import test.*

fun box(): String {
    return Entity("OK").directed().calc().value
}