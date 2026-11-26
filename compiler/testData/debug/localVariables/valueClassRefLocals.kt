// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

@JvmInline
value class IntHolder(val value: Int)

@JvmInline
value class StringHolder(val value: String)

fun call(f: () -> Unit) = f()

// A lambda changes the variables, so they are stored in a `Ref`, which keeps the source name
fun box() {
    var u: UInt = 1u
    var ih = IntHolder(2)
    var sh = StringHolder("a")
    call {
        u = 3u
        ih = IntHolder(4)
        sh = StringHolder("b")
    }
    u.toInt() + ih.value + sh.value.length
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:15 box:
// test.kt:16 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef
// test.kt:17 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef
// test.kt:18 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:11 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:19 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:20 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:21 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:22 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:11 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:23 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:24 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef

// EXPECTATIONS JVM
// test.kt:15 box:
// test.kt:16 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef
// test.kt:17 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef
// test.kt:18 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:11 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:19 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:20 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:21 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:22 box$lambda$0: $u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, $sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:11 call: f:kotlin.jvm.functions.Function0=TestKt$<lambda>
// test.kt:23 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
// test.kt:24 box: u:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, ih:kotlin.jvm.internal.Ref$IntRef=kotlin.jvm.internal.Ref$IntRef, sh:kotlin.jvm.internal.Ref$ObjectRef=kotlin.jvm.internal.Ref$ObjectRef
