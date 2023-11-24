// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// FIR status: KT-46419, ILT conversions to Byte and Short are not supported by design
// WITH_STDLIB
// This test exists only to check that we don't accidentally break the buggy behavior of the old JVM backend in JVM IR (KT-42321).
// Feel free to remove it as soon as there's no language version where such code is allowed (KT-38895).

// FILE: test.kt

import kotlin.reflect.KClass

class K<L>(val type: KClass<out Number>) {
    fun check(o: L, description: String, expected: KClass<out Number> = Int::class) {
        val x = o as Any
        if (x::class != expected) {
            throw AssertionError("Fail K<${type.simpleName}> $description: " +
                                         "expected ${expected.qualifiedName}, actual ${x::class.qualifiedName}")
        }
    }
}

fun box(): String {
    val kl = K<Long>(Long::class)
    kl.check(1.plus(2), "plus", Int::class)
    kl.check(1.minus(2), "minus", Int::class)
    kl.check(1.times(2), "times", Int::class)
    kl.check(1.div(2), "div", Int::class)
    kl.check(1.rem(2), "rem", Int::class)
    kl.check(1.unaryPlus(), "unaryPlus", Int::class)
    kl.check(1.unaryMinus(), "unaryMinus", Int::class)
    kl.check(1.shl(2), "shl", Int::class)
    kl.check(1.shr(2), "shr", Int::class)
    kl.check(1.ushr(2), "ushr", Int::class)
    kl.check(1.and(2), "and", Int::class)
    kl.check(1.or(2), "or", Int::class)
    kl.check(1.xor(2), "xor", Int::class)
    kl.check(1.inv(), "inv", Int::class)

    kl.check(1 + 2, "plus via operator", Long::class)
    kl.check(1 - 2, "minus via operator", Long::class)
    kl.check(1 * 2, "times via operator", Long::class)
    kl.check(1 / 2, "div via operator", Long::class)
    kl.check(1 % 2, "rem via operator", Long::class)
    kl.check(+1, "unaryPlus via operator", Long::class)
    kl.check(-1, "unaryMinus via operator", Long::class)
    kl.check(1 shl 2, "shl infix", Long::class)
    kl.check(1 shr 2, "shr infix", Long::class)
    kl.check(1 ushr 2, "ushr infix", Long::class)
    kl.check(1 and 2, "and infix", Long::class)
    kl.check(1 or 2, "or infix", Long::class)
    kl.check(1 xor 2, "xor infix", Long::class)

    val ks = K<Short>(Short::class)
    ks.check(1.plus(2), "plus", Int::class)
    ks.check(1.minus(2), "minus", Int::class)
    ks.check(1.times(2), "times", Int::class)
    ks.check(1.div(2), "div", Int::class)
    ks.check(1.rem(2), "rem", Int::class)
    ks.check(1.unaryPlus(), "unaryPlus", Int::class)
    ks.check(1.unaryMinus(), "unaryMinus", Int::class)
    ks.check(1.shl(2), "shl", Int::class)
    ks.check(1.shr(2), "shr", Int::class)
    ks.check(1.ushr(2), "ushr", Int::class)
    ks.check(1.and(2), "and", Int::class)
    ks.check(1.or(2), "or", Int::class)
    ks.check(1.xor(2), "xor", Int::class)
    ks.check(1.inv(), "inv", Int::class)

    ks.check(1 + 2, "plus via operator", Short::class)
    ks.check(1 - 2, "minus via operator", Short::class)
    ks.check(1 * 2, "times via operator", Short::class)
    ks.check(1 / 2, "div via operator", Short::class)
    ks.check(1 % 2, "rem via operator", Short::class)
    ks.check(+1, "unaryPlus via operator", Short::class)
    ks.check(-1, "unaryMinus via operator", Short::class)
    ks.check(1 shl 2, "shl infix", Short::class)
    ks.check(1 shr 2, "shr infix", Short::class)
    ks.check(1 ushr 2, "ushr infix", Short::class)
    ks.check(1 and 2, "and infix", Short::class)
    ks.check(1 or 2, "or infix", Short::class)
    ks.check(1 xor 2, "xor infix", Short::class)

    val kb = K<Byte>(Byte::class)
    kb.check(1.plus(2), "plus", Int::class)
    kb.check(1.minus(2), "minus", Int::class)
    kb.check(1.times(2), "times", Int::class)
    kb.check(1.div(2), "div", Int::class)
    kb.check(1.rem(2), "rem", Int::class)
    kb.check(1.unaryPlus(), "unaryPlus", Int::class)
    kb.check(1.unaryMinus(), "unaryMinus", Int::class)
    kb.check(1.shl(2), "shl", Int::class)
    kb.check(1.shr(2), "shr", Int::class)
    kb.check(1.ushr(2), "ushr", Int::class)
    kb.check(1.and(2), "and", Int::class)
    kb.check(1.or(2), "or", Int::class)
    kb.check(1.xor(2), "xor", Int::class)
    kb.check(1.inv(), "inv", Int::class)

    kb.check(1 + 2, "plus via operator", Byte::class)
    kb.check(1 - 2, "minus via operator", Byte::class)
    kb.check(1 * 2, "times via operator", Byte::class)
    kb.check(1 / 2, "div via operator", Byte::class)
    kb.check(1 % 2, "rem via operator", Byte::class)
    kb.check(+1, "unaryPlus via operator", Byte::class)
    kb.check(-1, "unaryMinus via operator", Byte::class)
    kb.check(1 shl 2, "shl infix", Byte::class)
    kb.check(1 shr 2, "shr infix", Byte::class)
    kb.check(1 ushr 2, "ushr infix", Byte::class)
    kb.check(1 and 2, "and infix", Byte::class)
    kb.check(1 or 2, "or infix", Byte::class)
    kb.check(1 xor 2, "xor infix", Byte::class)

    val jl = J<Long>(Long::class)
    jl.check(1.plus(2), "plus", Int::class)
    jl.check(1.minus(2), "minus", Int::class)
    jl.check(1.times(2), "times", Int::class)
    jl.check(1.div(2), "div", Int::class)
    jl.check(1.rem(2), "rem", Int::class)
    jl.check(1.unaryPlus(), "unaryPlus", Int::class)
    jl.check(1.unaryMinus(), "unaryMinus", Int::class)
    jl.check(1.shl(2), "shl", Int::class)
    jl.check(1.shr(2), "shr", Int::class)
    jl.check(1.ushr(2), "ushr", Int::class)
    jl.check(1.and(2), "and", Int::class)
    jl.check(1.or(2), "or", Int::class)
    jl.check(1.xor(2), "xor", Int::class)
    jl.check(1.inv(), "inv", Int::class)

    jl.check(1 + 2, "plus via operator", Long::class)
    jl.check(1 - 2, "minus via operator", Long::class)
    jl.check(1 * 2, "times via operator", Long::class)
    jl.check(1 / 2, "div via operator", Long::class)
    jl.check(1 % 2, "rem via operator", Long::class)
    jl.check(+1, "unaryPlus via operator", Long::class)
    jl.check(-1, "unaryMinus via operator", Long::class)
    jl.check(1 shl 2, "shl infix", Long::class)
    jl.check(1 shr 2, "shr infix", Long::class)
    jl.check(1 ushr 2, "ushr infix", Long::class)
    jl.check(1 and 2, "and infix", Long::class)
    jl.check(1 or 2, "or infix", Long::class)
    jl.check(1 xor 2, "xor infix", Long::class)

    val js = J<Short>(Short::class)
    js.check(1.plus(2), "plus", Int::class)
    js.check(1.minus(2), "minus", Int::class)
    js.check(1.times(2), "times", Int::class)
    js.check(1.div(2), "div", Int::class)
    js.check(1.rem(2), "rem", Int::class)
    js.check(1.unaryPlus(), "unaryPlus", Int::class)
    js.check(1.unaryMinus(), "unaryMinus", Int::class)
    js.check(1.shl(2), "shl", Int::class)
    js.check(1.shr(2), "shr", Int::class)
    js.check(1.ushr(2), "ushr", Int::class)
    js.check(1.and(2), "and", Int::class)
    js.check(1.or(2), "or", Int::class)
    js.check(1.xor(2), "xor", Int::class)
    js.check(1.inv(), "inv", Int::class)

    js.check(1 + 2, "plus via operator", Short::class)
    js.check(1 - 2, "minus via operator", Short::class)
    js.check(1 * 2, "times via operator", Short::class)
    js.check(1 / 2, "div via operator", Short::class)
    js.check(1 % 2, "rem via operator", Short::class)
    js.check(+1, "unaryPlus via operator", Short::class)
    js.check(-1, "unaryMinus via operator", Short::class)
    js.check(1 shl 2, "shl infix", Short::class)
    js.check(1 shr 2, "shr infix", Short::class)
    js.check(1 ushr 2, "ushr infix", Short::class)
    js.check(1 and 2, "and infix", Short::class)
    js.check(1 or 2, "or infix", Short::class)
    js.check(1 xor 2, "xor infix", Short::class)

    val jb = J<Byte>(Byte::class)
    jb.check(1.plus(2), "plus", Int::class)
    jb.check(1.minus(2), "minus", Int::class)
    jb.check(1.times(2), "times", Int::class)
    jb.check(1.div(2), "div", Int::class)
    jb.check(1.rem(2), "rem", Int::class)
    jb.check(1.unaryPlus(), "unaryPlus", Int::class)
    jb.check(1.unaryMinus(), "unaryMinus", Int::class)
    jb.check(1.shl(2), "shl", Int::class)
    jb.check(1.shr(2), "shr", Int::class)
    jb.check(1.ushr(2), "ushr", Int::class)
    jb.check(1.and(2), "and", Int::class)
    jb.check(1.or(2), "or", Int::class)
    jb.check(1.xor(2), "xor", Int::class)
    jb.check(1.inv(), "inv", Int::class)

    jb.check(1 + 2, "plus via operator", Byte::class)
    jb.check(1 - 2, "minus via operator", Byte::class)
    jb.check(1 * 2, "times via operator", Byte::class)
    jb.check(1 / 2, "div via operator", Byte::class)
    jb.check(1 % 2, "rem via operator", Byte::class)
    jb.check(+1, "unaryPlus via operator", Byte::class)
    jb.check(-1, "unaryMinus via operator", Byte::class)
    jb.check(1 shl 2, "shl infix", Byte::class)
    jb.check(1 shr 2, "shr infix", Byte::class)
    jb.check(1 ushr 2, "ushr infix", Byte::class)
    jb.check(1 and 2, "and infix", Byte::class)
    jb.check(1 or 2, "or infix", Byte::class)
    jb.check(1 xor 2, "xor infix", Byte::class)

    return "OK"
}

// FILE: J.java

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;

public class J<M> {
    private final KClass<? extends Number> type;

    public J(KClass<? extends Number> type) {
        this.type = type;
    }

    public void check(M x, String description, KClass<? extends Number> expected) {
        KClass<?> actual = JvmClassMappingKt.getKotlinClass(x.getClass());
        if (!actual.equals(expected)) {
            throw new AssertionError("Fail J<" + type.getSimpleName() + "> " + description + ": " +
                    "expected: " + expected.getQualifiedName() + ", actual: " + actual.getQualifiedName());
        }
    }
}
