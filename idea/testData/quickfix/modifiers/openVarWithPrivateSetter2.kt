// "Remove 'private' modifier" "true"
open class My {
    open var foo = 42
        <caret>private set
}
/* FIR_COMPARISON */