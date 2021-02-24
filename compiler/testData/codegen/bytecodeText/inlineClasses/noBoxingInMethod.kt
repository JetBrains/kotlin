inline class IC(val value: Int) {
    inline fun toLong(): Long = this.value.toLong()
}

// 0 INVOKESTATIC IC\.box-impl