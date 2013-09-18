class Z{

    fun a(s: Int) {}

    fun b() {
        Z().(Z::a)(1)
    }
}

// 2 invoke \(LZ;I\)V