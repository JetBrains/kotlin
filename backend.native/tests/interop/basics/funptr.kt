import kotlinx.cinterop.*
import cfunptr.*

fun main(args: Array<String>) {
    val atoiPtr = getAtoiPtr()!!

    val getPrintIntPtrPtr = getGetPrintIntPtrPtr()!!
    val printIntPtr = getPrintIntPtrPtr()!!.reinterpret<CFunction<(Int) -> Unit>>()

    val fortyTwo = memScoped {
        atoiPtr("42".cstr.getPointer(memScope))
    }

    printIntPtr(fortyTwo)

    printIntPtr(
            getDoubleToIntPtr()!!(
                    getAddPtr()!!(5.1, 12.2)
            )
    )

    val isIntPositivePtr = getIsIntPositivePtr()!!

    printIntPtr(isIntPositivePtr(42).ifThenOneElseZero())
    printIntPtr(isIntPositivePtr(-42).ifThenOneElseZero())
}

fun Boolean.ifThenOneElseZero() = if (this) 1 else 0