fun test1() {}

fun test2() {
    test1()
}

// 2 INVOKESTATIC _DefaultPackage-1-[0-9a-f]+\.test1 \(\)V
// 1 INVOKESTATIC _DefaultPackage-1-[0-9a-f]+\.test2 \(\)V
