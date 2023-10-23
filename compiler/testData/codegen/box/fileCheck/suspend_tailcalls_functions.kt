// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

suspend fun sUnit(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    println("sUnit")
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

suspend fun sInt(): Int = suspendCoroutineUninterceptedOrReturn { x ->
    println("sInt")
    x.resume(42)
    COROUTINE_SUSPENDED
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s1#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s1() {
    // CHECK-NOT: call void @"kfun:$s1COROUTINE${{[0-9]*}}#<init>
    println("s1")
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s2#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s2() {
    // CHECK-NOT: call void @"kfun:$s2COROUTINE${{[0-9]*}}#<init>
    println("s2")
    sUnit()
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s3#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s3() {
    // CHECK-NOT: call void @"kfun:$s3COROUTINE${{[0-9]*}}#<init>
    println("s3")
    sUnit()
    return
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s4#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s4(): Int {
    // CHECK-NOT: call void @"kfun:$s4COROUTINE${{[0-9]*}}#<init>
    println("s4")
    return sInt()
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s5#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s5() {
    // CHECK-NOT: call void @"kfun:$s5COROUTINE${{[0-9]*}}#<init>
    println("s5")
    run { sUnit() }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s6#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s6() {
    // CHECK-NOT: call void @"kfun:$s6COROUTINE${{[0-9]*}}#<init>
    run {
        println("s6")
        sUnit()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s7#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s7(): Int {
    // CHECK-NOT: call void @"kfun:$s7COROUTINE${{[0-9]*}}#<init>
    return run {
        println("s7")
        sInt()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s8#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s8(): Int {
    // CHECK-NOT: call void @"kfun:$s8COROUTINE${{[0-9]*}}#<init>
    run {
        println("s8")
        return sInt()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s9#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s9() {
    // CHECK-NOT: call void @"kfun:$s9COROUTINE${{[0-9]*}}#<init>
    run {
        println("s9-1")
        run {
            println("s9-2")
            sUnit()
        }
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s10#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s10(): Int {
    // CHECK-NOT: call void @"kfun:$s10COROUTINE${{[0-9]*}}#<init>
    run {
        println("s10-1")
        return run {
            println("s10-2")
            sInt()
        }
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s11#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s11() {
    // CHECK-NOT: call void @"kfun:$s11COROUTINE${{[0-9]*}}#<init>
    println("s11")
    sUnit()
    return
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s12#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s12() {
    // CHECK-NOT: call void @"kfun:$s12COROUTINE${{[0-9]*}}#<init>
    run {
        println("s12")
        sUnit()
        return
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s13#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s13() {
    // CHECK-NOT: call void @"kfun:$s13COROUTINE${{[0-9]*}}#<init>
    run {
        println("s13")
        sUnit()
    }
    return
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s14#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s14() {
    // CHECK-NOT: call void @"kfun:$s14COROUTINE${{[0-9]*}}#<init>
    run {
        println("s14")
        sUnit()
        return@run
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s15#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
@Suppress("UNREACHABLE_CODE")
suspend fun s15(): Int {
    // CHECK-NOT: call void @"kfun:$s15COROUTINE${{[0-9]*}}#<init>
    run {
        println("s15-1")
        return run {
            println("s15-2")
            return sInt()
        }
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s16#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s16(): Int {
    // CHECK-NOT: call void @"kfun:$s16COROUTINE${{[0-9]*}}#<init>
    run outer@ {
        println("s16-1")
        return run inner@ {
            println("s16-2")
            return@inner sInt()
        }
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s17#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
@Suppress("UNREACHABLE_CODE")
suspend fun s17(): Int {
    // CHECK-NOT: call void @"kfun:$s17COROUTINE${{[0-9]*}}#<init>
    return run outer@ {
        println("s17-1")
        return run inner@ {
            println("s17-2")
            return@outer sInt()
        }
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s18#suspend(kotlin.Boolean;kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s18(f: Boolean) {
    // CHECK-NOT: call void @"kfun:$s18COROUTINE${{[0-9]*}}#<init>
    if (f) {
        println("s18-1")
        sUnit()
    } else {
        println("s18-2")
        s2()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s19#suspend(kotlin.Boolean;kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s19(f: Boolean): Int {
    // CHECK-NOT: call void @"kfun:$s19COROUTINE${{[0-9]*}}#<init>
    if (f) {
        println("s19-1")
        return sInt()
    } else {
        println("s19-2")
        return s4()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s20#suspend(kotlin.Boolean;kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s20(f: Boolean): Int {
    // CHECK-NOT: call void @"kfun:$s20COROUTINE${{[0-9]*}}#<init>
    return if (f) {
        println("s20-1")
        sInt()
    } else {
        println("s20-2")
        s4()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s21#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s21() {
    // CHECK-NOT: call void @"kfun:$s21COROUTINE${{[0-9]*}}#<init>
    try {
        println("s21")
    } catch (t: Throwable) {
        sUnit()
    }
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s22#suspend(kotlin.coroutines.Continuation<kotlin.Int>){}kotlin.Any
suspend fun s22(): Int {
    // CHECK-NOT: call void @"kfun:$s22COROUTINE${{[0-9]*}}#<init>
    try {
        println("s22")
    } catch (t: Throwable) {
        return sInt()
    }
    return s4()
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s23#suspend(kotlin.Boolean;kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s23(f: Boolean) {
    // CHECK-NOT: call void @"kfun:$s23COROUTINE${{[0-9]*}}#<init>
    val x = run {
        if (f) {
            println("s23")
            sUnit()
            return
        }
        42
    }
    println(x)
}
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#s24#suspend(kotlin.coroutines.Continuation<kotlin.Unit>){}kotlin.Any
suspend fun s24() {
    // CHECK: call void @"kfun:$s24COROUTINE${{[0-9]*}}.<init>#internal
    sInt()
}
// CHECK-LABEL: epilogue:

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    builder {
        s1()
        s2()
        s3()
        s4()
        s5()
        s6()
        s7()
        s8()
        s9()
        s10()
        s11()
        s12()
        s13()
        s14()
        s15()
        s16()
        s17()
        s18(true)
        s19(true)
        s20(true)
        s21()
        s22()
        s23(true)
        s24()
    }
    return "OK"
}