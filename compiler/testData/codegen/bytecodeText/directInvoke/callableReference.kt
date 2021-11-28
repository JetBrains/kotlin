class Z{

    fun a(s: Int) {}

    fun b() {
        val cr = (Z::a)
        cr(Z(), 1)
    }
}

// 1 invoke \(LZ;I\)V
