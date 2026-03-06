// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses

@JvmInline
value class A<T : Any>(val x: List<T>)

@JvmInline
value class B(val x: UInt)

@JvmInline
value class C(val x: Int, val y: B, val z: String)

@JvmInline
value class D(val x: C) {
    constructor(x: Int, y: UInt, z: Int) : this(C(x, B(y), z.toString()))

    init {
        println(x.x)
    }
}

class Regular(private val x: D) {
    fun privateAccess() {
        listOf(x.x.x)
        listOf(x.x)
        listOf(x)
    }
}

fun functionWithoutBoxes(x: D, y: D) {
    var z: D = x
    val t: D = D(C(1, B(3U), "4"))
    z = t
    require(z == y)
}

// 2 public static toString-impl\(IILjava/lang/String;\)Ljava/lang/String;
// 2 INVOKESTATIC C.toString-impl \(IILjava/lang/String;\)Ljava/lang/String;
// 1 INVOKESTATIC D.toString-impl \(IILjava/lang/String;\)Ljava/lang/String;
// 2 public static hashCode-impl\(IILjava/lang/String;\)I
// 2 INVOKESTATIC C.hashCode-impl \(IILjava/lang/String;\)I
// 1 INVOKESTATIC D.hashCode-impl \(IILjava/lang/String;\)I
// 2 public static equals-impl\(IILjava/lang/String;Ljava/lang/Object;\)Z
// 2 public final static equals-impl0\(IILjava/lang/String;IILjava/lang/String;\)Z
// 1 public final static constructor-impl\(III\)LD;
// 2 public final static constructor-impl\(IILjava/lang/String;\)V
// 2 INVOKESTATIC D.constructor-impl \(IILjava/lang/String;\)V
// 2 INVOKESTATIC C.constructor-impl \(IILjava/lang/String;\)V
// 1 public final getX\(\)LC;
// 1 public final synthetic unbox-impl-x\(\)I
// 1 public final synthetic unbox-impl-y\(\)I
// 1 public final synthetic unbox-impl-z\(\)Ljava/lang/String;
// 2 private synthetic <init>\(IILjava/lang/String;\)V
// 1 public final static synthetic box-impl\(IILjava/lang/String;\)LD;
// 1 public final static synthetic box-impl\(IILjava/lang/String;\)LC;
// 1 public final synthetic unbox-impl-x\(\)LC;
// 1 public final synthetic unbox-impl-x-x\(\)I
// 1 public final synthetic unbox-impl-x-y\(\)I
// 1 public final synthetic unbox-impl-x-z\(\)Ljava/lang/String;
// 2 private final I x\n
// 1 private final I y\n
// 1 private final Ljava/lang/String; z\n
// 0 private final LC; (.*)x\n
// 1 private final I x-x\n
// 1 private final I x-y\n
// 1 private final Ljava/lang/String; x-z\n
// 1 INVOKESPECIAL C.<init> \(IILjava/lang/String;\)V
// 1 INVOKESPECIAL D.<init> \(IILjava/lang/String;\)V
// 2 INVOKESTATIC D.box-impl \(IILjava/lang/String;\)LD;
// 3 INVOKESTATIC C.box-impl \(IILjava/lang/String;\)LC;
// 1 public final static functionWithoutBoxes-d7Y4ALo\(IILjava/lang/String;IILjava/lang/String;\)V
// 0 functionWithoutBoxes.*(\n {3}.*)*(\n {4}(NEW [ABCD]|.*(box|[ABCD]\.<init>|LA;|LB;|LC;|LD;)))
// 1 privateAccess.*(\n   .+)*(\n    GETFIELD Regular\.x-x-x : I)(\n   .+)*(\n    INVOKEVIRTUAL Regular\.getX-x \(\)LC;)(\n   .+)*(\n    INVOKESPECIAL Regular\.getX \(\)LD;)
