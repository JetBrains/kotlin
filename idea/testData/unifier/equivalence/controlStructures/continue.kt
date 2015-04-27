fun foo(a: Int) {
    A@
    while (true) {
        B@
        while (true) {
            if (a > 0) <selection>continue@A</selection>
            if (a < 0) continue@B
        }

        B@
        while (true) {
            if (a > 0) continue@A
            if (a < 0) continue@B
        }
    }
}