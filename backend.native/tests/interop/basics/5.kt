import platform.posix.printf

val golden = immutableBinaryBlobOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21, 0x00).asCPointer(0)

fun main(args: Array<String>) {
    printf("%s\n", golden)
}
