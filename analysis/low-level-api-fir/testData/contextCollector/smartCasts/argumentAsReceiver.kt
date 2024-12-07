interface Foo {}
interface Bar {}

fun test(obj: Any) {
    if (obj is Foo) {
        if (obj is Bar) {
            <expr>obj.consume(obj)</expr>
        }
    }
}

fun Bar.consume(obj: Foo) {}