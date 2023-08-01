interface Foo {}
interface Bar {}

fun test(obj: Any) {
    if (obj is Foo) {
        if (obj is Bar) {
            obj.consume(<expr>obj</expr>)
        }
    }
}

fun Bar.consume(obj: Foo) {}