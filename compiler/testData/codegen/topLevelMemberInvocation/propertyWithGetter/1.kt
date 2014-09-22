package a

val prop: Int = 0
    get() {
        return $prop + 1
    }

// 2 INVOKESTATIC a/APackage.+\.getProp \(\)I
// 1 GETSTATIC a/APackage.+\.prop \: I
