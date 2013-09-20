fun Int.test1() {}

fun test2() {
    1.test1()
}

// 2 INVOKESTATIC _DefaultPackage-1-[0-9a-f]+\.test1 \(I\)V
// 1 INVOKESTATIC _DefaultPackage-1-[0-9a-f]+\.test2 \(\)V
