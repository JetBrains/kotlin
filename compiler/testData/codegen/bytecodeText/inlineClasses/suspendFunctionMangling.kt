// KT-41374
// !LANGUAGE: +InlineClasses
// FILE: test.kt

package a

inline class P(val i: Int)

suspend fun foo(p: P = P(1)) {}

suspend fun bar(p: P) {}

// The mangled name for a suspend function includes the continuation parameter in the hash computation, but not the
// default argument mask and handler.

// 1 public final static foo-zMJ7EWY\(ILkotlin/coroutines/Continuation;\)Ljava/lang/Object;
// 1 public static synthetic foo-zMJ7EWY\$default\(ILkotlin/coroutines/Continuation;ILjava/lang/Object;\)Ljava/lang/Object;
// 1 INVOKESTATIC a/TestKt.foo-zMJ7EWY \(ILkotlin/coroutines/Continuation;\)Ljava/lang/Object;
// 1 public final static bar-zMJ7EWY\(ILkotlin/coroutines/Continuation;\)Ljava/lang/Object;