// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class A<T : Any>(val x: List<T>)

@JvmInline
value class B(val x: UInt)

@JvmInline
value class C(val x: Int, val y: B, val z: String = "3")

@JvmInline
value class D(val x: C) {
    constructor(x: Int, y: UInt, z: Int) : this(C(x, B(y), z.toString()))

    init {
        println(x.x)
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
// 1 public final getX\$x\(\)I
// 1 public final getX\$y\(\)I
// 1 public final getX\$z\(\)Ljava/lang/String;
// 2 private synthetic <init>\(IILjava/lang/String;\)V
// 1 public final static synthetic box-impl\(IILjava/lang/String;\)LD;
// 1 public final static synthetic box-impl\(IILjava/lang/String;\)LC;
// 2 public final synthetic unbox-impl0\(\)I
// 2 public final synthetic unbox-impl1\(\)I
// 2 public final synthetic unbox-impl2\(\)Ljava/lang/String;
// 1 private I x\$x
// 1 private I x\$y
// 1 private Ljava/lang/String; x\$z
// 0 private LC; x
// 3 private I x
// 1 private I y
// 1 private Ljava/lang/String; z
// 1 INVOKESPECIAL C.<init> \(IILjava/lang/String;\)V
// 1 INVOKESPECIAL D.<init> \(IILjava/lang/String;\)V
// 1 INVOKESTATIC D.box-impl \(IILjava/lang/String;\)LD;
// 1 INVOKESTATIC D.box-impl \(IILjava/lang/String;\)LD;\n    ARETURN
// 1 INVOKESTATIC C.box-impl \(IILjava/lang/String;\)LC;
// 1 INVOKESTATIC C.box-impl \(IILjava/lang/String;\)LC;\n    ARETURN
// 1 public final static functionWithoutBoxes-GPBa7dw-AYZo7_8\(IILjava/lang/String;IILjava/lang/String;\)V
// 0 functionWithoutBoxes.*(\n {3}.*)*(\n {4}(NEW [ABCD]|.*(box|[ABCD]\.<init>|LA;|LB;|LC;|LD;)))
