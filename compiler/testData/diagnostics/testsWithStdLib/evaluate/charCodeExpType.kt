const val p1: Int = '\n'.code
const val p2: Long = '\n'.code.toLong()
const val p3: Byte = '\n'.code.toByte()
const val p4: Short = '\n'.code.toShort()

const val e2: Long = <!TYPE_MISMATCH!>'\n'.code<!>
const val e3: Byte = <!TYPE_MISMATCH!>'\n'.code<!>
const val e4: Short = <!TYPE_MISMATCH!>'\n'.code<!>
