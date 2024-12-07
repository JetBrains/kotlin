// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// WITH_STDLIB

class MyColor(val x: Int, val y: Int, val z: Int)

class Some {
    val delegate by lazy { MyColor(0x12 /* constant = 18 */, 2, 3) }

    val lambda = lazy { MyColor(1, 2, 3) }

    val nonLazy = MyColor(1, 2, 3)
}
