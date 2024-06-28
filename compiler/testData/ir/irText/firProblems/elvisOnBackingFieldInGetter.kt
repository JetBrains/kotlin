// ISSUE: KT-61974
// TARGET_BACKEND: JVM

class Test {
    var resourceTable: ResourceTable? = null
        get() {
            if (field != null) {
                return field
            }
            val fileData = getFileData()
            if (fileData != null) {
                field = ResourceTable()
            }
            return field ?: ResourceTable()
        }

    fun getFileData(): String? = ""
}

class ResourceTable
