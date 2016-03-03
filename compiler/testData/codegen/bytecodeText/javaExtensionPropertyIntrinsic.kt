class Klass

fun foo() {
    // Even though no intrinsic is used,
    // redundant boxing/unboxing optimizes out wrapping/unrapping java.lang.Class instances
    val c0 = (Klass::class).java // LDC LKlass;.class

    val c1 = Klass::class.java // LDC LKlass;.class

    val c2 = Int::class.java // GETSTATIC java/lang/Integer.TYPE

    val c3 = Integer::class.java // LDC Ljava/lang/Integer;.class
}

// 2 LDC LKlass;.class
// 1 GETSTATIC java/lang/Integer.TYPE : Ljava/lang/Class;
// 0 INVOKESTATIC kotlin/jvm.*\.getJava
// 1 LDC Ljava/lang/Integer;.class
