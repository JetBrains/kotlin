package a

val prop: Int = 0
    get() {
        return $prop + 1
    }

// 2 INVOKESTATIC a/A1Kt.getProp \(\)I
// 1 GETSTATIC a/A1Kt.prop \: I
