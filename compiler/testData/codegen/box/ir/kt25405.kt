// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun <T> tableView(init: Table<T>.() -> Unit) {
    Table<T>().init()
}

var result = "fail"

class Table<T> {

    inner class TableColumn(val name: String) {

    }

    fun column(name: String, init: TableColumn.() -> Unit) {
        val column = TableColumn(name).init()
    }
}

fun foo() {
    tableView<String> {
        column("OK") {
            result = name
        }
    }
}

fun box(): String {
    foo()
    return result
}