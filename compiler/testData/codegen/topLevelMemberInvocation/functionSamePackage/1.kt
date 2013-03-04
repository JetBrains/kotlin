fun test1() {}

fun test2() {
    test1()
}

// 2 INVOKESTATIC _DefaultPackage\$src\$1\$[\-]*[0-9]*\.test1 \(\)V
// 1 INVOKESTATIC _DefaultPackage\$src\$1\$[\-]*[0-9]*\.test2 \(\)V