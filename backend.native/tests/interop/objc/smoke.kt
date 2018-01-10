import kotlinx.cinterop.*
import objcSmoke.*

fun main(args: Array<String>) {
    autoreleasepool {
        run()
    }
}

fun run() {
    println(
            getSupplier(
                    invoke1(42) { it * 2 }
            )!!()
    )

    val foo = Foo()

    val classGetter = getClassGetter(foo)
    invoke2 { println(classGetter()) }

    foo.hello()
    foo.name = "everybody"
    foo.helloWithPrinter(object : NSObject(), PrinterProtocol {
        override fun print(string: CPointer<ByteVar>?) {
            println("Kotlin says: " + string?.toKString())
        }
    })

    Bar().hello()

    val pair = MutablePairImpl(42, 17)
    replacePairElements(pair, 1, 2)
    pair.swap()
    println("${pair.first}, ${pair.second}")

    // equals and hashCode (virtually):
    val map = mapOf(foo to pair, pair to foo)

    // equals (directly):
    if (!foo.equals(pair)) {
        // toString (directly):
        println(map[pair].toString() + map[foo].toString() == foo.description() + pair.description())
    }

    // hashCode (directly):
    if (foo.hashCode() == foo.hash().toInt()) {
        // toString (virtually):
        println(map.keys.map { it.toString() }.min() == foo.description())
    }

    println(globalString)
    autoreleasepool {
        globalString = "Another global string"
    }
    println(globalString)

    println(globalObject)
    globalObject = object : NSObject() {
        override fun description() = "global object"
    }
    println(globalObject)

    println(formatStringLength("%d %d", 42, 17))
}

fun MutablePairProtocol.swap() {
    update(0, add = second)
    update(1, sub = first)
    update(0, add = second)
    update(1, sub = second*2)
}

class Bar : Foo() {
    override fun helloWithPrinter(printer: PrinterProtocol?) = memScoped {
        printer!!.print("Hello from Kotlin".cstr.getPointer(memScope))
    }
}

@Suppress("CONFLICTING_OVERLOADS")
class MutablePairImpl(first: Int, second: Int) : NSObject(), MutablePairProtocol {
    private var elements = intArrayOf(first, second)

    override fun first() = elements.first()
    override fun second() = elements.last()

    override fun update(index: Int, add: Int) {
        elements[index] += add
    }

    override fun update(index: Int, sub: Int) {
        elements[index] -= sub
    }
}