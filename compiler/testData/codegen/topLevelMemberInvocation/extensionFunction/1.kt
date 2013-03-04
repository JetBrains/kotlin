fun Int.test1() {}

fun test2() {
    1.test1()
}

// 2 INVOKESTATIC _DefaultPackage\$src\$1\$[\-]*[0-9]*\.test1 \(I\)V
// 1 INVOKESTATIC _DefaultPackage\$src\$1\$[\-]*[0-9]*\.test2 \(\)V