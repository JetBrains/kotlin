class Z{

    fun a(s: Int) {}

    fun b() {
        (Z::a)(Z(), 1)
    }
}

// 1 invoke \(LZ;I\)V
