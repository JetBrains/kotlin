// Parenthesis are not valid in names in the dex file format.
// Therefore, do not attempt to dex this file as it will fail.
// See: https://source.android.com/devices/tech/dalvik/dex-format#simplename
// IGNORE_DEXING
class `(X)` {
    fun `(Y)`() {}
}

// One in the file name, one in the class header, two in local variables in the constructor and the method, and one in kotlin.Metadata.d2
// 5 \(X\)

// One in the method header and one in kotlin.Metadata.d2
// 2 \(Y\)
