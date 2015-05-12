interface Printer {
    fun print()
}

class OKPrinter : Printer {
    override fun print() {  }
}

class MyClass(var printer: Printer)


fun main(m: MyClass) {
    if (m.printer is OKPrinter) {
        // We do not need smart cast here, so we should not get SMARTCAST_IMPOSSIBLE
        m.printer.print()
    }
}