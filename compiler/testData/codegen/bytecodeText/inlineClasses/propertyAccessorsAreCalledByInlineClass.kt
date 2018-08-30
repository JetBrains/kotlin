// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int) {
    val aVal: Int
        get() = x

    var aVar: Int
        get() = x
        set(v) {}

    val String.extVal: Int
        get() = x

    var String.extVar: Int
        get() = x
        set(v) {}
}

// FILE: test.kt
fun Z.test() {
    aVal
    aVar
    aVar = 42
    aVar++

    "".extVal
    "".extVar
    "".extVar = 42
    "".extVar++
}

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.
// 0 INVOKESTATIC Z\-Erased\.
// 1 INVOKESTATIC Z.getAVal \(I\)I
// 2 INVOKESTATIC Z.getAVar \(I\)I
// 2 INVOKESTATIC Z.setAVar \(II\)V
// 1 INVOKESTATIC Z.getExtVal \(ILjava/lang/String;\)I
// 2 INVOKESTATIC Z.getExtVar \(ILjava/lang/String;\)I
// 2 INVOKESTATIC Z.setExtVar \(ILjava/lang/String;I\)V