// "Remove 'private' modifier" "true"
abstract class My {
    abstract var foo: Int
        <caret>private set
}
/* FIR_COMPARISON */