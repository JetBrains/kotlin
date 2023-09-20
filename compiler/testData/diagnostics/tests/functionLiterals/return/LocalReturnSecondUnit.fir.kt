val flag = true

val a = b@ {
    if (flag) return@b 4
    return@b
}
