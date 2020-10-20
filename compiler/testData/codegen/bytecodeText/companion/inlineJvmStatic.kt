class C {
    companion object {
        @JvmStatic
        inline fun f() = "OK"
    }
}

// in C$Companion.f:
// 1 LDC "OK"
// in static C.f:
// 1 INVOKEVIRTUAL C\$Companion.f
