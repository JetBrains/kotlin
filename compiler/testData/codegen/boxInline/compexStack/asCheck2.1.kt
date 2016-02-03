import test.*

fun box(): String {
    return ContentTypeByExtension.processRecords { ext -> ext }
}