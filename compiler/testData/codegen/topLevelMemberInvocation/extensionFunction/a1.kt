fun Int.test1() {}

fun test2() {
    1.test1()
}

// 2 INVOKESTATIC A1Kt.test1 \(I\)V
// 1 INVOKESTATIC A1Kt.test2 \(\)V
