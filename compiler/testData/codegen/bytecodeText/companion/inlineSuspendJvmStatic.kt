class C {
    companion object {
        @JvmStatic
        suspend inline fun f() = "OK"
    }
}

// in C$Companion.f:
// 2 LDC "OK"
// 1 final f\$\$forInline
// in static C.f:
// 1 INVOKEVIRTUAL C\$Companion.f
