fun Int.test1() {}

fun test2() {
    1.test1()
}

// 2 INVOKESTATIC _DefaultPackage.+\.test1 \(I\)V
// 1 INVOKESTATIC _DefaultPackage.+\.test2 \(\)V
