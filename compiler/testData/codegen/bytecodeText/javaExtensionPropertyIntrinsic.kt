class Klass

fun foo() {
    val c0 = (Klass::class).java // prevent intrinsic .java for class literal
    val c1 = Klass::class.java
    val c2 = Int::class.java
    val c3 = Integer::class.java

}

// 2 LDC LKlass;.class
// 1 GETSTATIC java/lang/Integer.TYPE : Ljava/lang/Class;
// 1 INVOKESTATIC kotlin/jvm.*\.getJava
// 1 LDC Ljava/lang/Integer;.class
