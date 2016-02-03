import test.*

fun box(): String {
    return processRecords { ext -> ext + "K" }
}