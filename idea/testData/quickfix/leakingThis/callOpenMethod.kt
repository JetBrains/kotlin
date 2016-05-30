// "Make 'init' final" "true"

open class My {

    init {
        <caret>init()
    }

    open fun init() {}
}