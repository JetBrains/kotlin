// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

var counter = 0

@JvmInline
value class MFVC(val x: Int, val y: Int) {

    fun equals(other: MFVC): Boolean {
        counter++
        return x == other.x && this.y == other.y
    }

    override fun equals(other: Any?): Boolean {
        counter++
        if (other !is MFVC) {
            return false
        }
        return equals(other)
    }

    override fun hashCode(): Int {
        counter++
        return x + 13 * y
    }
}

fun box(): String {
    val mfvc1 = MFVC(1, 2)
    val mfvc2 = MFVC(3, 4)
    
    require(mfvc1.equals(mfvc1))
    require(!mfvc1.equals(mfvc2))
    require(!mfvc2.equals(mfvc1))
    require(mfvc2.equals(mfvc2))
    
    require(mfvc1.equals(mfvc1 as Any?))
    require(!mfvc1.equals(mfvc2 as Any?))
    require(!mfvc2.equals(mfvc1 as Any?))
    require(mfvc2.equals(mfvc2 as Any?))
    
    require(mfvc1.hashCode() == 27)
    require(mfvc2.hashCode() == 55)
    
    require(counter == 4 + 2 * 4 + 2)
    
    return "OK"
}
