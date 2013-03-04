package a

val prop: Int = 0
    get() {
        return $prop + 1
    }

// 2 INVOKESTATIC a/APackage\$src\$1\$[\-]*[0-9]*\.getProp \(\)I
// 1 GETSTATIC a/APackage\$src\$1\$[\-]*[0-9]*\.prop \: I